package org.teiid.translator.wfs;

import javax.resource.cci.ConnectionFactory;
import org.teiid.language.QueryExpression;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.WSConnection;

public abstract class WfsExecutionFactory extends ExecutionFactory<ConnectionFactory, WSConnection>{

    @Override
    public void getMetadata(MetadataFactory metadataFactory,
                            WSConnection conn)
            throws TranslatorException {
        WfsMetadataProcessor mp = new WfsMetadataProcessor();
        mp.process(metadataFactory, conn);
    }

    @Override
    public ResultSetExecution createResultSetExecution(QueryExpression command,
                                                       ExecutionContext executionContext,
                                                       RuntimeMetadata metadata,
                                                       WSConnection connection)
            throws TranslatorException {
        return new WfsResultsExecution();
    }

}
