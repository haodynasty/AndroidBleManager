package com.blakequ.bluetooth_manager_lib.connect;

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
    private UUID characteristicNotificationUUID;
    private Type operatorType;

    private BluetoothSubScribeData(UUID characteristicUUID, byte[] characteristicValue, UUID descriptorUUID
        ,byte[] descriptorValue, UUID characteristicNotificationUUID, Type operatorType){
        this.characteristicNotificationUUID = characteristicNotificationUUID;
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

    private BluetoothSubScribeData(UUID characteristicUUID, byte[] characteristicValue, Type operatorType){
        this.characteristicUUID = characteristicUUID;
        this.operatorType = operatorType;
    }

    public UUID getCharacteristicNotificationUUID() {
        return characteristicNotificationUUID;
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
        private UUID characteristicNotificationUUID;
        private Type operatorType;

        public Builder setCharacteristicUUID(UUID characteristicUUID){
            if (characteristicUUID == null){
                throw new IllegalArgumentException("invalid null characteristic UUID");
            }
            this.characteristicUUID = characteristicUUID;
            return this;
        }

        public Builder setCharacteristicValue(byte[] characteristicValue){
            if (characteristicValue == null){
                throw new IllegalArgumentException("invalid null characteristic value");
            }
            this.characteristicValue = characteristicValue;
            return this;
        }

        public Builder setDescriptorUUID(UUID descriptorUUID){
            if (descriptorUUID == null){
                throw new IllegalArgumentException("invalid null descriptor UUID");
            }
            this.descriptorUUID = descriptorUUID;
            return this;
        }

        public Builder setDescriptorValue(byte[] descriptorValue){
            if (descriptorValue == null){
                throw new IllegalArgumentException("invalid null descriptor value");
            }
            this.descriptorValue = descriptorValue;
            return this;
        }

        public Builder setCharacteristicNotificationUUID(UUID characteristicNotificationUUID){
            if (characteristicNotificationUUID == null){
                throw new IllegalArgumentException("invalid null characteristic notification UUID");
            }
            this.characteristicNotificationUUID = characteristicNotificationUUID;
            return this;
        }

        public Builder setOperatorType(Type type){
            this.operatorType = type;
            return this;
        }

        public BluetoothSubScribeData build(){
            //check params
            if (operatorType == null){
                throw new IllegalArgumentException("invalid Type, and type can not be null");
            }
            if (characteristicUUID == null){
                throw new IllegalArgumentException("invalid characteristic, and characteristic can not be null");
            }
            BluetoothSubScribeData data = null;
            switch (operatorType){
                case CHAR_READ:
                    data = new BluetoothSubScribeData(characteristicUUID, Type.CHAR_READ);
                    break;
                case CHAR_WIRTE:
                    if (characteristicValue == null){
                        throw new IllegalArgumentException("invalid null characteristic value");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, characteristicValue, Type.CHAR_READ);
                    break;
                case DESC_READ:
                    if (descriptorUUID == null){
                        throw new IllegalArgumentException("invalid null descriptor UUID");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, characteristicValue, descriptorUUID, descriptorValue, characteristicNotificationUUID, operatorType);
                    break;
                case DESC_WRITE:
                    if (descriptorUUID == null || descriptorValue == null){
                        throw new IllegalArgumentException("invalid null descriptor UUID or value");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, characteristicValue, descriptorUUID, descriptorValue, characteristicNotificationUUID, operatorType);
                    break;
                case NOTIFY:
                    if (descriptorUUID == null){
                        throw new IllegalArgumentException("invalid null descriptor UUID");
                    }
                    if (characteristicNotificationUUID == null){
                        throw new IllegalArgumentException("invalid null characteristic notification UUID");
                    }
                    data = new BluetoothSubScribeData(characteristicUUID, characteristicValue, descriptorUUID, descriptorValue, characteristicNotificationUUID, operatorType);
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
