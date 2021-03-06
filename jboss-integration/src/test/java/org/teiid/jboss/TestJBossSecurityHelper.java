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

package org.teiid.jboss;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;

import junit.framework.TestCase;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.plugins.JBossSecurityContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.teiid.adminapi.impl.SessionMetadata;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.client.security.InvalidSessionException;
import org.teiid.dqp.service.SessionServiceException;
import org.teiid.net.socket.AuthenticationType;
import org.teiid.security.Credentials;
import org.teiid.services.SessionServiceImpl;
import org.teiid.vdb.runtime.VDBKey;

@SuppressWarnings("nls")
public class TestJBossSecurityHelper extends TestCase {

    private JBossSecurityHelper buildSecurityHelper(final String domain, final SecurityDomainContext sdc) 
            throws Exception {
    	Principal p = Mockito.mock(Principal.class);
    	Mockito.stub(p.getName()).toReturn("alreadylogged"); //$NON-NLS-1$
    	HashSet<Principal> principals = new HashSet<Principal>();
    	principals.add(p);
    	
    	final Subject subject = new Subject(false, principals, new HashSet(), new HashSet());
    	@SuppressWarnings("serial")
        JBossSecurityHelper sh = new JBossSecurityHelper() {
    	    @Override
    	    public Subject getSubjectInContext(String securityDomain) {
    	        if (securityDomain.equals("passthrough")) {
    	            return subject;
    	        }
    	        return null;
    	    }
    	    
    	    @Override
            protected SecurityDomainContext getSecurityDomainContext(String securityDomain) {
    	        if (securityDomain.equals(domain)) {
    	            return sdc;
    	        }
    	        return null;
    	    }    	
    	};
    	return sh;
    }
       
    public void testAuthenticate() throws Exception {
    	Credentials credentials = new Credentials("pass1".toCharArray());

    	String domains = "testFile";
        final SecurityDomainContext securityContext = Mockito.mock(SecurityDomainContext.class);
        
    	JBossSecurityHelper ms = buildSecurityHelper(domains, securityContext);
        
        AuthenticationManager authManager = new AuthenticationManager() {
			public String getSecurityDomain() {
				return null;
			}
			public boolean isValid(Principal principal, Object credential, Subject activeSubject) {
				return true;
			}
			public boolean isValid(Principal principal, Object credential) {
				return true;
			}
			@Override
			public Principal getTargetPrincipal(Principal anotherDomainPrincipal, Map<String, Object> contextMap) {
				return null;
			}
			@Override
			public Subject getActiveSubject() {
				return null;
			}
            @Override
            public void logout(Principal arg0, Subject arg1) {
            }
		};
        
        Mockito.stub(securityContext.getAuthenticationManager()).toReturn(authManager);
        
        Object c = ms.authenticate(domains, "user1", credentials, null); //$NON-NLS-1$ 
        assertTrue(c instanceof JBossSecurityContext); //$NON-NLS-1$
        assertEquals(domains, ((JBossSecurityContext)c).getSecurityDomain());
    }
    
	public void validateSession(boolean securityEnabled) throws Exception {
		final ArrayList<String> domains = new ArrayList<String>();
		domains.add("somedomain");				
	
        final SecurityDomainContext securityContext = Mockito.mock(SecurityDomainContext.class);
        
        AuthenticationManager authManager = Mockito.mock(AuthenticationManager.class);
        Mockito.stub(authManager.isValid(new SimplePrincipal("steve"), "pass1", new Subject())).toReturn(true);
        
        Mockito.stub(securityContext.getAuthenticationManager()).toReturn(authManager);
		
        SessionServiceImpl jss = new SessionServiceImpl() {
        	@Override
        	protected VDBMetaData getActiveVDB(String vdbName, String vdbVersion)
        			throws SessionServiceException {
        		return Mockito.mock(VDBMetaData.class);
        	}
        };
        jss.setSecurityHelper(buildSecurityHelper("somedomain", securityContext));
		jss.setSecurityDomain("somedomain");
		
		try {
			jss.validateSession(String.valueOf(1));
			fail("exception expected"); //$NON-NLS-1$
		} catch (InvalidSessionException e) {
			
		}
		
		SessionMetadata info = jss.createSession("x", "1", AuthenticationType.USERPASSWORD, "steve",  new Credentials("pass1".toCharArray()), "foo", new Properties()); //$NON-NLS-1$ //$NON-NLS-2$
		if (securityEnabled) {
			Mockito.verify(authManager).isValid(new SimplePrincipal("steve"), "pass1", new Subject()); 
		}
		
		String id1 = info.getSessionId();
		jss.validateSession(id1);
		
		assertEquals(1, jss.getActiveSessionsCount());
		assertEquals(0, jss.getSessionsLoggedInToVDB(new VDBKey("a", 1)).size()); //$NON-NLS-1$ 
		
		jss.closeSession(id1);
		
		try {
			jss.validateSession(id1);
			fail("exception expected"); //$NON-NLS-1$
		} catch (InvalidSessionException e) {
			
		}
		
		try {
			jss.closeSession(id1);
			fail("exception expected"); //$NON-NLS-1$
		} catch (InvalidSessionException e) {
			
		}
	}
	
	@Test public void testvalidateSession() throws Exception{
		validateSession(true);
	}

	@Test public void testvalidateSession2() throws Exception {
		validateSession(false);
	}    
}
