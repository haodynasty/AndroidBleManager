package com.blakequ.bluetooth_manager_lib.connect;

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
 * date     : 2016/8/29 14:04 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class ConnectConfig {
    public static int maxConnectDeviceNum = 5;//一次最大连接个数

    /**线性间隔重连，每次断开后重连时间是线性增长*/
    public static final int RECONNECT_LINEAR = 1;
    /**指数间隔重新*/
    public static final int RECONNECT_EXPONENT = 2;
    /**先线性后指数重新*/
    public static final int RECONNECT_LINE_EXPONENT = 3;
    /**固定时间重连,每次断开后都是相同时间之后发起重连*/
    public static final int RECONNECT_FIXED_TIME = 4;
}
