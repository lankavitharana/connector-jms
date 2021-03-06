/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.net.jms.actions;

import org.ballerinalang.bre.BallerinaTransactionContext;
import org.ballerinalang.bre.BallerinaTransactionManager;
import org.ballerinalang.bre.Context;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BConnector;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.nativeimpl.actions.ClientConnectorFuture;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.Attribute;
import org.ballerinalang.natives.annotations.BallerinaAction;
import org.ballerinalang.natives.annotations.BallerinaAnnotation;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.jms.Constants;
import org.ballerinalang.net.jms.JMSTransactionContext;
import org.ballerinalang.net.jms.JMSUtils;
import org.ballerinalang.util.DistributedTxManagerProvider;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.transport.jms.contract.JMSClientConnector;
import org.wso2.carbon.transport.jms.exception.JMSConnectorException;
import org.wso2.carbon.transport.jms.impl.JMSConnectorFactoryImpl;
import org.wso2.carbon.transport.jms.sender.wrappers.SessionWrapper;
import org.wso2.carbon.transport.jms.utils.JMSConstants;

import java.util.Map;
import java.util.UUID;
import javax.jms.Message;
import javax.transaction.TransactionManager;

import static org.ballerinalang.net.jms.Constants.EMPTY_CONNECTOR_ID;

/**
 * {@code Post} is the send action implementation of the JMS Connector.
 */
@BallerinaAction(packageName = "ballerina.net.jms",
                 actionName = "send",
                 connectorName = Constants.CONNECTOR_NAME,
                 args = {
                         @Argument(name = "jmsClientConnector",
                                   type = TypeKind.CONNECTOR), @Argument(name = "destinationName",
                                                                         type = TypeKind.STRING),
                         @Argument(name = "msgType",
                                   type = TypeKind.STRING), @Argument(name = "m",
                                                                      type = TypeKind.MESSAGE)
                 },
                 returnType = { @ReturnType(type = TypeKind.BOOLEAN) },
                 connectorArgs = {
                         @Argument(name = "properties",
                                   type = TypeKind.MAP)
                 })
@BallerinaAnnotation(annotationName = "Description",
                     attributes = {
                             @Attribute(name = "value",
                                        value = "SEND action implementation of the JMS Connector")
                     })
@BallerinaAnnotation(annotationName = "Param",
                     attributes = {
                             @Attribute(name = "connector",
                                        value = "Connector")
                     })
@BallerinaAnnotation(annotationName = "Param",
                     attributes = {
                             @Attribute(name = "destinationName",
                                        value = "Destination Name")
                     })
@BallerinaAnnotation(annotationName = "Param",
                     attributes = {
                             @Attribute(name = "msgType",
                                        value = "Message Type")
                     })
@BallerinaAnnotation(annotationName = "Param",
                     attributes = {
                             @Attribute(name = "message",
                                        value = "Message")
                     })
public class Send extends AbstractJMSAction {
    private static final Logger log = LoggerFactory.getLogger(Send.class);

    @Override
    public ConnectorFuture execute(Context context) {

        // Extract argument values
        BConnector bConnector = (BConnector) getRefArgument(context, 0);
        BStruct messageStruct = ((BStruct) getRefArgument(context, 1));
        String destination = getStringArgument(context, 0);

        Message jmsMessage = JMSUtils.getJMSMessage(messageStruct);

        validateParams(bConnector);

        // Get the map of properties.
        BStruct  connectorConfig = ((BStruct) bConnector.getRefField(0));

        Map<String, String> propertyMap = JMSUtils.preProcessJmsConfig(connectorConfig);

        // Generate connector the key, if its not already generated
        String connectorKey;
        if (EMPTY_CONNECTOR_ID.equals(bConnector.getStringField(0))) {
            connectorKey = UUID.randomUUID().toString();
            bConnector.setStringField(0, connectorKey);
        } else {
            connectorKey = bConnector.getStringField(0);
        }

        propertyMap.put(JMSConstants.PARAM_DESTINATION_NAME, destination);

        boolean isTransacted = Boolean.FALSE;
        if (propertyMap.get(JMSConstants.PARAM_ACK_MODE) != null) {
            isTransacted = (JMSConstants.SESSION_TRANSACTED_MODE.equals(propertyMap.get(JMSConstants.PARAM_ACK_MODE))
                    || JMSConstants.XA_TRANSACTED_MODE.equals(propertyMap.get(JMSConstants.PARAM_ACK_MODE))) && context
                    .isInTransaction();
        }

        try {
            JMSClientConnector jmsClientConnector = new JMSConnectorFactoryImpl().createClientConnector(propertyMap);
            if (log.isDebugEnabled()) {
                log.debug("Sending JMS Message to " + propertyMap.get(JMSConstants.PARAM_DESTINATION_NAME));
            }
            if (!isTransacted) {
                jmsClientConnector.send(jmsMessage, destination);
            } else {
                SessionWrapper sessionWrapper;
                BallerinaTransactionManager ballerinaTxManager = context.getBallerinaTransactionManager();
                BallerinaTransactionContext txContext = ballerinaTxManager.getTransactionContext(connectorKey);
                // if transaction initialization has not yet been done
                // (if this is the first transacted action happens from this particular connector with this
                // transaction block)
                if (txContext == null) {
                    sessionWrapper = jmsClientConnector.acquireSession();
                    txContext = new JMSTransactionContext(sessionWrapper, jmsClientConnector);
                    //Handle XA initialization
                    if (txContext.getXAResource() != null) {
                        initializeXATransaction(ballerinaTxManager);
                    }
                    ballerinaTxManager.registerTransactionContext(connectorKey, txContext);
                } else {
                    sessionWrapper = ((JMSTransactionContext) txContext).getSessionWrapper();
                }
                jmsClientConnector.sendTransactedMessage(jmsMessage, destination, sessionWrapper);
            }
        } catch (JMSConnectorException e) {
            throw new BallerinaException("Failed to send message. " + e.getMessage(), e, context);
        }
        ClientConnectorFuture future = new ClientConnectorFuture();
        future.notifySuccess();
        return future;
    }

    private void initializeXATransaction(BallerinaTransactionManager ballerinaTxManager) {
        /* Atomikos transaction manager initialize only distributed transaction is present.*/
        if (!ballerinaTxManager.hasXATransactionManager()) {
            TransactionManager transactionManager = DistributedTxManagerProvider.getInstance().getTransactionManager();
            ballerinaTxManager.setXATransactionManager(transactionManager);
        }
        if (!ballerinaTxManager.isInXATransaction()) {
            ballerinaTxManager.beginXATransaction();
        }
    }
}
