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
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Using the Schematic (https://github.com/SimonVT/schematic) library to define
 * the Reddit Post table columns in a content provider backed by a database.
 */

public interface PostContract {
    @DataType(INTEGER)
    @PrimaryKey
    @AutoIncrement
    String _ID = "_id";

    @DataType(TEXT)
    @Unique(onConflict = ConflictResolutionType.REPLACE)
    @NotNull
    String COLUMN_POST_ID = "post_id";

    @DataType(TEXT)
    @NotNull
    String COLUMN_POST_LINK = "post_link";

    @DataType(INTEGER)
    @NotNull
    String COLUMN_POST_VOTES = "post_votes";

    @DataType(INTEGER)
    @NotNull
    String COLUMN_POST_COMMENTS = "post_comments";

    @DataType(INTEGER)
    @NotNull
    String COLUMN_POST_UNREAD = "post_unread";

    @DataType(INTEGER)
    @NotNull
    String COLUMN_POST_FAVORITE = "post_favorite";
}
