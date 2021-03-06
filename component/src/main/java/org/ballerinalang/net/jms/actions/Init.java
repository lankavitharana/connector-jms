/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.jms.actions;

import org.ballerinalang.bre.Context;
import org.ballerinalang.connector.api.AbstractNativeAction;
import org.ballerinalang.connector.api.ConnectorFuture;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.nativeimpl.actions.ClientConnectorFuture;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.Attribute;
import org.ballerinalang.natives.annotations.BallerinaAction;
import org.ballerinalang.natives.annotations.BallerinaAnnotation;
import org.ballerinalang.net.jms.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * {@code Init} is the Init action implementation of the JMS Connector.
 *
 * @since 0.9
 */
@BallerinaAction(packageName = "ballerina.net.jms",
                 actionName = "<init>",
                 connectorName = Constants.CONNECTOR_NAME,
                 args = {
                         @Argument(name = "c",
                                   type = TypeKind.CONNECTOR)
                 },
                 connectorArgs = {
                         @Argument(name = "properties",
                                   type = TypeKind.STRUCT)
                 })
@BallerinaAnnotation(annotationName = "Description",
                     attributes = {
                             @Attribute(name = "value",
                                        value = "The init action implementation for JMS connector.")
                     })
@Component(name = "action.net.jms.init",
           immediate = true,
           service = AbstractNativeAction.class)
public class Init extends AbstractJMSAction {

    @Override
    public ConnectorFuture execute(Context context) {
        ClientConnectorFuture future = new ClientConnectorFuture();
        future.notifySuccess();
        return future;
    }

    @Override
    public boolean isNonBlockingAction() {
        return false;
    }
}
