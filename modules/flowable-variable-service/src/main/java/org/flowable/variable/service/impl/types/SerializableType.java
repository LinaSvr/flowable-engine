/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.variable.service.impl.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.util.IoUtil;
import org.flowable.engine.common.impl.util.ReflectUtil;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Marcus Klimstra (CGI)
 */
public class SerializableType extends ByteArrayType {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializableType.class);

    public static final String TYPE_NAME = "serializable";

    protected boolean trackDeserializedObjects;

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    public SerializableType() {

    }

    public SerializableType(boolean trackDeserializedObjects) {
        this.trackDeserializedObjects = trackDeserializedObjects;
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        Object cachedObject = valueFields.getCachedValue();
        if (cachedObject != null) {
            return cachedObject;
        }

        byte[] bytes = (byte[]) super.getValue(valueFields);
        if (bytes != null) {

            Object deserializedObject = deserialize(bytes, valueFields);
            valueFields.setCachedValue(deserializedObject);

            if (trackDeserializedObjects && valueFields instanceof VariableInstanceEntity) {
                Context.getCommandContext().addCloseListener(new VerifyDeserializedObjectCommandContextCloseListener(
                        new DeserializedObject(this, valueFields.getCachedValue(), bytes, (VariableInstanceEntity) valueFields)));
            }

            return deserializedObject;
        }
        return null; // byte array is null
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        byte[] bytes = serialize(value, valueFields);
        valueFields.setCachedValue(value);

        super.setValue(bytes, valueFields);

        if (trackDeserializedObjects && valueFields instanceof VariableInstanceEntity) {
            Context.getCommandContext().addCloseListener(new VerifyDeserializedObjectCommandContextCloseListener(
                    new DeserializedObject(this, valueFields.getCachedValue(), bytes, (VariableInstanceEntity) valueFields)));
        }

    }

    public byte[] serialize(Object value, ValueFields valueFields) {
        if (value == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = createObjectOutputStream(baos);
            oos.writeObject(value);
        } catch (Exception e) {
            throw new FlowableException("Couldn't serialize value '" + value + "' in variable '" + valueFields.getName() + "'", e);
        } finally {
            IoUtil.closeSilently(oos);
        }
        return baos.toByteArray();
    }

    public Object deserialize(byte[] bytes, ValueFields valueFields) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = createObjectInputStream(bais);
            Object deserializedObject = ois.readObject();

            return deserializedObject;
        } catch (Exception e) {
            /*
            This is a workaround for tasks INPROCESS-265 и INPROCESS-266 to return null then problem with
            deserialization happened with serviceContext or one of its elements
             */
            if (isFromLiferay(valueFields)) {
                LOGGER.error("Couldn't deserialize object in variable '" + valueFields.getName() + "'", e);
            } else {
                throw new FlowableException("Couldn't deserialize object in variable '" + valueFields.getName() + "'", e);
            }
        } finally {
            IoUtil.closeSilently(bais);
        }
        return null;
    }

    /**
     *Check for deserializing object is one of specific objects from com.liferay.portal.kernel package
     */
    private boolean isFromLiferay(ValueFields valueFields) {
        String name = valueFields.getName();
        return "modelPermissions".equals(name) || "serviceContext".equals(name) || "portletPreferencesIds".equals(name);
    }

    @Override
    public boolean isAbleToStore(Object value) {
        // TODO don't we need null support here?
        return value instanceof Serializable;
    }

    protected ObjectInputStream createObjectInputStream(InputStream is) throws IOException {
        return new ObjectInputStream(is) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                return ReflectUtil.loadClass(desc.getName());
            }
        };
    }

    protected ObjectOutputStream createObjectOutputStream(OutputStream os) throws IOException {
        return new ObjectOutputStream(os);
    }
}
