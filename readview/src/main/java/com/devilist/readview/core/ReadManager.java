/*
 * Copyright  2019  zengp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devilist.readview.core;

import com.devilist.readview.ReadConfig;
import com.devilist.readview.core.Interface.IPage;

import java.util.List;

class ReadManager {

    private ReadConfig mConfig;

    private List<IPage> mPageList;

    private int mCurrentPage;

    public ReadManager() {
        mConfig = new ReadConfig();
    }

    public ReadConfig getConfig() {
        return mConfig;
    }

    public void setConfig(ReadConfig config) {
        this.mConfig = config;
    }

    public void loadText(String text) {

    }
}
