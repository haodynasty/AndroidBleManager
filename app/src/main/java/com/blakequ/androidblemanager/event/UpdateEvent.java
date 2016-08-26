package com.blakequ.androidblemanager.event;

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
 * date     : 2016/8/24 14:37 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class UpdateEvent {

    private Type type;
    private Object obj;
    private String msg;

    public UpdateEvent(Type type) {
        this.type = type;
    }

    public UpdateEvent(Type type, Object obj) {
        this.obj = obj;
        this.type = type;
    }

    public UpdateEvent(Type type, Object obj, String msg) {
        this.obj = obj;
        this.type = type;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public static enum Type{
        SCAN_UPDATE,
        BLE_DATA
    }
}
