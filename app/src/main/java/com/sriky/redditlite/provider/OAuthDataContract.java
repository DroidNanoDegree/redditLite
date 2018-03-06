/*
 * Copyright (C) 2017 Srikanth Basappa
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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.sriky.redditlite.provider;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Unique;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.REAL;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Using the Schematic (https://github.com/SimonVT/schematic) library to define
 * the OAuthData table columns in a content provider backed by a database.
 */

public interface OAuthDataContract {
    @DataType(INTEGER)
    @PrimaryKey
    @AutoIncrement
    String _ID = "_id";

    @DataType(TEXT)
    @Unique(onConflict = ConflictResolutionType.REPLACE)
    @NotNull
    String COLUMN_USERNAME = "username";

    @DataType(TEXT)
    @NotNull
    String COLUMN_SESSION_TOKEN = "session_token";

    @DataType(TEXT)
    @NotNull
    String COLUMN_REFRESH_TOKEN = "refresh_token";

    @DataType(TEXT)
    @NotNull
    String COLUMN_ACCESS_SCOPES = "access_scopes";

    @DataType(INTEGER)
    @NotNull
    String COLUMN_IS_EXPIRED = "is_expired";

    @DataType(REAL)
    @NotNull
    String COLUMN_EXPIRATION = "expiration";
}
