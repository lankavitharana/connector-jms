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
import org.ballerinalang.model.values.BBlob;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.jms.JMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;

/**
 * Get bytes content of the JMS Message.
 */
@BallerinaFunction(
        packageName = "ballerina.net.jms",
        functionName = "getBytesMessageContent",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "JMSMessage",
                             structPackage = "ballerina.net.jms"),
        returnType = {@ReturnType(type = TypeKind.BLOB)},
        isPublic = true
)
public class GetBytesMessageContent extends AbstractNativeFunction {

    private static final Logger log = LoggerFactory.getLogger(GetBytesMessageContent.class);

    public BValue[] execute(Context context) {

        BStruct messageStruct  = ((BStruct) this.getRefArgument(context, 0));
        Message jmsMessage = JMSUtils.getJMSMessage(messageStruct);

        byte[] messageContent = new byte[0];

        try {
            if (jmsMessage instanceof BytesMessage) {
                messageContent = new byte[getSize((BytesMessage) jmsMessage)];
                ((BytesMessage) jmsMessage).readBytes(messageContent);
            } else {
                log.error("JMSMessage is not a Bytes message. ");
            }
        } catch (JMSException e) {
            log.error("Error when retrieving JMS message content :" + e.getLocalizedMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug("Get content from the JMS message");
        }

        return this.getBValues(new BBlob(messageContent));
    }

    private int getSize(BytesMessage bytesMessage) throws JMSException {
        if (bytesMessage.getBodyLength() < Integer.MAX_VALUE) {
            return Math.toIntExact(bytesMessage.getBodyLength());
        }
        return Integer.MAX_VALUE;
    }
}
