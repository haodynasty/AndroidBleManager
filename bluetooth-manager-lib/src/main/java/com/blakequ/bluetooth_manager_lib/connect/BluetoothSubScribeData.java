package com.blakequ.bluetooth_manager_lib.connect;

import com.blakequ.bluetooth_manager_lib.device.resolvers.GattAttributeResolver;

import java.util.UUID;

/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p/>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/8/18 17:48 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class BluetoothSubScribeData {

    private UUID characteristicUUID;
    private byte[] characteristicValue;
    private UUID descriptorUUID;
    private byte[] descriptorValue;
    //the notification uuid for Characteristic
    private Type operatorType;

    private BluetoothSubScribeData(UUID characteristicUUID, byte[] characteristicValue, UUID descriptorUUID
        ,byte[] descriptorValue, Type operatorType){
        this.characteristicUUID = characteristicUUID;
        this.characteristicValue = characteristicValue;
        this.descriptorUUID = descriptorUUID;
        this.descriptorValue = descriptorValue;
        this.operatorType = operatorType;
    }

    private BluetoothSubScribeData(UUID characteristicUUID, Type operatorType){
        this.characteristicUUID = characteristicUUID;
        this.operatorType = operatorType;
    }


    public UUID getCharacteristicUUID() {
        return characteristicUUID;
    }

    public byte[] getCharacteristicValue() {
        return characteristicValue;
    }

    public UUID getDescriptorUUID() {
        return descriptorUUID;
    }

    public byte[] getDescriptorValue() {
        return descriptorValue;
    }

    public Type getOperatorType() {
        return operatorType;
    }

    public static final class Builder {
        private UUID characteristicUUID;
        private byte[] characteristicValue;
        private UUID descriptorUUID;
        private byte[] descriptorValue;
        //the notification uuid for Characteristic
        private Type operatorType;

        /**
         * read Characteristic
         * @param characteristicUUID
         * @return
         */
        public Builder setCharacteristicRead(UUID characteristicUUID){
            this.operatorType = Type.CHAR_READ;
            this.characteristicUUID = characteristicUUID;
            return this;
        }

        /**
         * write Characteristic
         * @param characteristicUUID
         * @param characteristicValue
         * @return
         */
        public Builder setCharacteristicWrite(UUID characteristicUUID, byte[] characteristicValue){
            this.operatorType = Type.CHAR_WIRTE;
            this.characteristicUUID = characteristicUUID;
            this.characteristicValue = characteristicValue;
            return this;
        }

        /**
         * read Descriptor
         * @param characteristicUUID
         * @param descriptorUUID
         * @return
         */
        public Builder setDescriptorRead(UUID characteristicUUID, UUID descriptorUUID){
            this.operatorType = Type.DESC_READ;
            this.characteristicUUID = characteristicUUID;
            this.descriptorUUID = descriptorUUID;
            return this;
        }

        /**
         * write Descriptor
         * @param characteristicUUID
         * @param descriptorUUID
         * @param descriptorValue
         * @return
         */
        public Builder setDescriptorWrite(UUID characteristicUUID, UUID descriptorUUID, byte[] descriptorValue){
            this.operatorType = Type.DESC_WRITE;
            this.characteristicUUID = characteristicUUID;
            this.descriptorUUID = descriptorUUID;
            this.descriptorValue = descriptorValue;
            return this;
        }

        /**
         * get notify
         * @param characteristicNotificationUUID notify characteristic uuid
         * @return
         */
        public Builder setCharacteristicNotify(UUID characteristicNotificationUUID){
            this.operatorType = Type.NOTIFY;
            this.characteristicUUID = characteristicNotificationUUID;
            this.descriptorUUID = UUID.fromString(GattAttributeResolver.CLIENT_CHARACTERISTIC_CONFIG);
            return this;
        }

        public BluetoothSubScribeData build(){
            if (characteristicUUID == null){
                throw new IllegalArgumentException("invalid characteristic, and characteristic can not be null");
            }
            BluetoothSubScribeData data = null;
            switch (operatorType){
                case CHAR_READ:
                    data = new BluetoothSubScribeData(characteristicUUID, operatorType);
                    break;
                case CHAR_WIRTE:
                    if (characteristicValue == null){
                        throw new IllegalArgumentException("invalid null characteristic value");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, characteristicValue, null,null, operatorType);
                    break;
                case DESC_READ:
                    if (descriptorUUID == null){
                        throw new IllegalArgumentException("invalid null descriptor UUID");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, null, descriptorUUID, null, operatorType);
                    break;
                case DESC_WRITE:
                    if (descriptorUUID == null || descriptorValue == null){
                        throw new IllegalArgumentException("invalid null descriptor UUID or value");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, null, descriptorUUID, descriptorValue, operatorType);
                    break;
                case NOTIFY:
                    if (descriptorUUID == null){
                        throw new IllegalArgumentException("invalid null descriptor UUID");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, null, descriptorUUID, null, operatorType);
                    break;
            }
            return data;
        }
    }

    /**
     * bluetooth subscribe operator type
     */
    public static enum Type{
        CHAR_WIRTE,
        CHAR_READ,
        DESC_WRITE,
        DESC_READ,
        NOTIFY
    }
}
