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

package org.ballerinalang.net.jms.nativeimpl.message;

import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BFloat;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.jms.JMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * Get Float Property from the JMS Message.
 */
@BallerinaFunction(
        packageName = "ballerina.net.jms",
        functionName = "getFloatProperty",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "JMSMessage",
                             structPackage = "ballerina.net.jms"),
        args = {@Argument(name = "propertyName", type = TypeKind.STRING)},
        returnType = {@ReturnType(type = TypeKind.FLOAT)},
        isPublic = true
)
public class GetFloatProperty extends AbstractNativeFunction {

    private static final Logger log = LoggerFactory.getLogger(GetFloatProperty.class);

    public BValue[] execute(Context context) {

        BStruct messageStruct  = ((BStruct) this.getRefArgument(context, 0));
        String propertyName = this.getStringArgument(context, 0);

        Message jmsMessage = JMSUtils.getJMSMessage(messageStruct);

        Float propertyValue = Float.valueOf(0);
        try {
            propertyValue = jmsMessage.getFloatProperty(propertyName);
        } catch (JMSException e) {
            log.error("Error when retrieving the float property :" + e.getLocalizedMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug("Get float property" + propertyName + " from message with value: " + propertyValue);
        }

        return this.getBValues(new BFloat(propertyValue));
    }
}
