/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.query.processor;

import static org.junit.Assert.*;
import static org.teiid.query.processor.TestProcessor.*;

import java.util.List;

import org.junit.Test;
import org.teiid.common.buffer.TupleSource;
import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidException;
import org.teiid.dqp.internal.process.DQPWorkContext;
import org.teiid.metadata.MetadataStore;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Table;
import org.teiid.query.mapping.relational.QueryNode;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.optimizer.TestOptimizer;
import org.teiid.query.optimizer.capabilities.BasicSourceCapabilities;
import org.teiid.query.optimizer.capabilities.DefaultCapabilitiesFinder;
import org.teiid.query.optimizer.capabilities.SourceCapabilities.Capability;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.unittest.RealMetadataFactory;
import org.teiid.query.util.CommandContext;

@SuppressWarnings("nls")
public class TestSourceHints {

	@Test public void testUserQueryHint() {
		String sql = "SELECT /*+ sh:'foo' bar:'leading' */ e1 from pm1.g1 order by e1 limit 1"; //$NON-NLS-1$
		
		ProcessorPlan plan = helpGetPlan(sql, RealMetadataFactory.example1Cached());
        
        List<?>[] expected = new List[] {};
        helpProcess(plan, manager("foo", "leading"), expected);
	}
	
	@Test public void testWithHint() {
		String sql = "WITH x as /*+ no_inline */ (SELECT /*+ sh:'x' */ e1 from pm1.g2) " +
				"SELECT /*+ sh:'foo' bar:'leading' */ g1.e1 from pm1.g1, x where g1.e1 = x.e1 order by g1.e1 limit 1"; //$NON-NLS-1$
		
		ProcessorPlan plan = helpGetPlan(sql, RealMetadataFactory.example1Cached());
        
        List<?>[] expected = new List[] {};
        helpProcess(plan, manager("foo x", "leading", "foo", "leading"), expected);
	}
	
	@Test public void testWithHintPushdown() throws TeiidException {
		String sql = "WITH x as /*+ no_inline */ (SELECT /*+ sh:'x' */ e1 from pm1.g2) " +
				"SELECT /*+ sh:'foo' bar:'leading' */ g1.e1 from pm1.g1, x where g1.e1 = x.e1 order by g1.e1 limit 1"; //$NON-NLS-1$
		
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.COMMON_TABLE_EXPRESSIONS, true);
		CommandContext context = new CommandContext();
		context.setDQPWorkContext(new DQPWorkContext());
		context.getDQPWorkContext().getSession().setVdb(RealMetadataFactory.example1VDB());
		ProcessorPlan plan = helpGetPlan(helpParse(sql), RealMetadataFactory.example1Cached(), new DefaultCapabilitiesFinder(caps), context);
        
        List<?>[] expected = new List[] {};
        helpProcess(plan, manager("foo x", "leading"), expected);
	}
	
	@Test public void testUnionHintPushdown() throws TeiidException {
		String sql = "SELECT /*+ sh:'foo' bar:'leading' */ g1.e1 from pm1.g1 " +
				"UNION ALL SELECT * from (SELECT /*+ sh:'x' bar:'z' */ g1.e1 from pm1.g1) as x"; //$NON-NLS-1$
		
        BasicSourceCapabilities caps = TestOptimizer.getTypicalCapabilities();
        caps.setCapabilitySupport(Capability.QUERY_UNION, true);
		CommandContext context = new CommandContext();
		context.setDQPWorkContext(new DQPWorkContext());
		context.getDQPWorkContext().getSession().setVdb(RealMetadataFactory.example1VDB());
		ProcessorPlan plan = helpGetPlan(helpParse(sql), RealMetadataFactory.example1Cached(), new DefaultCapabilitiesFinder(caps), context);
        
        List<?>[] expected = new List[] {};
        helpProcess(plan, manager("foo x", "leading z"), expected);
	}
	
	@Test public void testKeepAliases() throws Exception {
		String sql = "SELECT /*+ sh KEEP ALIASES bar:'leading(g)' */ e1 from pm1.g1 g order by e1 limit 1"; //$NON-NLS-1$
		CommandContext cc = TestProcessor.createCommandContext();
		cc.setDQPWorkContext(new DQPWorkContext());
		cc.getDQPWorkContext().getSession().setVdb(RealMetadataFactory.example1VDB());
		ProcessorPlan plan = TestOptimizer.getPlan(TestOptimizer.helpGetCommand(sql, RealMetadataFactory.example1Cached(), null), RealMetadataFactory.example1Cached(), TestOptimizer.getGenericFinder(), null, true, cc);
		TestOptimizer.checkAtomicQueries(new String[] {"SELECT /*+sh KEEP ALIASES bar:'leading(g)' */ g.e1 AS c_0 FROM pm1.g1 AS g ORDER BY c_0"}, plan);
        
        List<?>[] expected = new List[] {};
        helpProcess(plan, manager(null, "leading(g)"), expected);
	}
	
	@Test public void testHintInView() {
    	MetadataStore metadataStore = new MetadataStore();
        Schema p1 = RealMetadataFactory.createPhysicalModel("p1", metadataStore); //$NON-NLS-1$
        Table t1 = RealMetadataFactory.createPhysicalGroup("t", p1); //$NON-NLS-1$
        RealMetadataFactory.createElements(t1, new String[] {"a", "b" }, new String[] { "string", "string" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        
        Schema v1 = RealMetadataFactory.createVirtualModel("v1", metadataStore); //$NON-NLS-1$
        QueryNode n1 = new QueryNode("SELECT /*+ sh:'x' */ a as c, b FROM p1.t"); //$NON-NLS-1$ //$NON-NLS-2$
        Table vt1 = RealMetadataFactory.createVirtualGroup("t1", v1, n1); //$NON-NLS-1$
        RealMetadataFactory.createElements(vt1, new String[] {"c", "b" }, new String[] { "string", "string" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        TransformationMetadata metadata = RealMetadataFactory.createTransformationMetadata(metadataStore, "metadata");
        
        //top level applies
        HardcodedDataManager manager = manager("foo x", "leading");
		
		String sql = "SELECT /*+ sh:'foo' bar:'leading' */ c from t1 order by c limit 1"; //$NON-NLS-1$
		ProcessorPlan plan = helpGetPlan(sql, metadata);
        
        List<?>[] expected = new List[] {};
        helpProcess(plan, manager, expected);
        
        //use the underlying hint
        manager = manager("x", null);
		sql = "SELECT c from t1 order by c limit 1"; //$NON-NLS-1$
		plan = helpGetPlan(sql, metadata);
        helpProcess(plan, manager, expected);
        
		sql = "SELECT c from t1 union all select c from t1"; //$NON-NLS-1$
		plan = helpGetPlan(sql, metadata);
        helpProcess(plan, manager, expected);
	}

	private HardcodedDataManager manager(final String ... hints) {
		HardcodedDataManager manager = new HardcodedDataManager() {
			int i = 0;
			@Override
			public TupleSource registerRequest(CommandContext context,
					Command command, String modelName,
					RegisterRequestParameter parameterObject)
					throws TeiidComponentException {
				if (hints[i*2] == null && hints[i*2+1] == null) {
					assertNull(command.getSourceHint());
				} else {
					assertEquals(hints[i*2], command.getSourceHint().getGeneralHint()); //$NON-NLS-1$
					assertEquals(hints[i*2+1], command.getSourceHint().getSourceHint("bar")); //$NON-NLS-1$
				}
				i = ++i%(hints.length/2);
				return CollectionTupleSource.createNullTupleSource();
			}
		};
		return manager;
	}
	
}
