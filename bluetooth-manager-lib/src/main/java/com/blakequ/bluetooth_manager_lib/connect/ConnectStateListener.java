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
 * date     : 2016/8/23 16:51 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public interface ConnectStateListener {

    /**
     * invoke when bluetooth connect state changed
     * @param address bluetooth device address
     * @param state current state
     */
    void onConnectStateChanged(String address, ConnectState state);
}
