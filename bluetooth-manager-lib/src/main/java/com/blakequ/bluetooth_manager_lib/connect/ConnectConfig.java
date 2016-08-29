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
    public static final long reconnectTime = 4000; //断开后等待尝试重新连接的时间ms
    public static final int reconnectedNum = 4; //断开后重新连接的次数
    public static final int maxConnectDeviceNum = 5;//一次最大连接个数
}
