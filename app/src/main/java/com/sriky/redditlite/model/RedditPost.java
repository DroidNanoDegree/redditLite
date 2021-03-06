/*
 * Copyright (C) 2018 Srikanth Basappa
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

package com.sriky.redditlite.model;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.sriky.redditlite.R;
import com.sriky.redditlite.provider.PostContract;

/**
 * Class to represent a Reddit post.
 */

public class RedditPost implements Parcelable {

    public static final Creator<RedditPost> CREATOR = new Creator<RedditPost>() {
        @Override
        public RedditPost createFromParcel(Parcel in) {
            return new RedditPost(in);
        }

        @Override
        public RedditPost[] newArray(int size) {
            return new RedditPost[size];
        }
    };
    //post type indexes.
    private static final int POST_TYPE_NO_MEDIA = 0;
    private static final int POST_TYPE_LINK = 1;
    private static final int POST_TYPE_IMAGE = 2;
    private static final int POST_TYPE_HOSTED_VIDEO = 3;
    private static final int POST_TYPE_RICH_VIDEO = 4;
    private static final int POST_TYPE_SELF = 5;
    //post thumbnail type indexes.
    private static final int DEFAULT = 0;
    private static final int SELF = 1;
    private static final int IMAGE = 2;
    private long mDate;
    private int mVotesCount;
    private int mCommentesCount;
    private PostType mType;
    private PostThumbnailType mPostThumbnailType;
    private String mPostId;
    private String mTitle;
    private String mAuthor;
    private String mSubreddit;
    private String mUrl;
    private String mPostDomain;
    private String mPostThumbnailUrl;
    private String mPostHint;
    private String mVideoUrl;
    private String mImageUrl;
    private String mBody;
    private String mSelfText;
    private boolean mVisited;
    private boolean mIsFavorite;

    protected RedditPost(Parcel in) {
        mDate = in.readLong();
        mVotesCount = in.readInt();
        mCommentesCount = in.readInt();
        mType = PostType.valueOf(in.readString());
        mPostThumbnailType = PostThumbnailType.valueOf(in.readString());
        mPostId = in.readString();
        mTitle = in.readString();
        mAuthor = in.readString();
        mSubreddit = in.readString();
        mUrl = in.readString();
        mPostDomain = in.readString();
        mPostThumbnailUrl = in.readString();
        mPostHint = in.readString();
        mVideoUrl = in.readString();
        mImageUrl = in.readString();
        mBody = in.readString();
        mSelfText = in.readString();
        mVisited = in.readByte() != 0;
        mIsFavorite = in.readByte() != 0;
    }

    public RedditPost(Cursor cursor, Context context) {
        mDate = cursor.getLong(cursor.getColumnIndex(PostContract.COLUMN_POST_DATE));
        mTitle = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_TITLE));
        mSubreddit = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_SUBREDDIT));
        mPostDomain = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_DOMAIN));
        mPostThumbnailUrl =
                cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_MEDIA_THUMBNAIL_URL));

        mVotesCount = cursor.getInt(cursor.getColumnIndex(PostContract.COLUMN_POST_VOTES));
        mCommentesCount = cursor.getInt(cursor.getColumnIndex(PostContract.COLUMN_POST_COMMENTS_COUNT));
        mPostId = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_ID));
        mUrl = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_REDDIT_URL));
        mAuthor = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_AUTHOR));
        mPostHint = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_HINT));
        mVideoUrl = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_VIDEO_URL));
        mImageUrl = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_IMAGE_URL));
        mBody = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_BODY));
        mSelfText = cursor.getString(cursor.getColumnIndex(PostContract.COLUMN_POST_SELF_TEXT));
        mVisited = cursor.getInt(cursor.getColumnIndex(PostContract.COLUMN_POST_VISITED)) > 0;
        mIsFavorite = cursor.getInt(cursor.getColumnIndex(PostContract.COLUMN_POST_FAVORITE)) > 0;

        if (TextUtils.isEmpty(mPostHint)) {
            mType = PostType.NO_MEDIA;
        } else {
            TypedArray postTypes = context.getResources().obtainTypedArray(R.array.postTypes);
            if (postTypes.getString(POST_TYPE_NO_MEDIA).equals(mPostHint)) {
                mType = PostType.NO_MEDIA;
            } else if (postTypes.getString(POST_TYPE_LINK).equals(mPostHint)) {
                mType = PostType.LINK;
            } else if (postTypes.getString(POST_TYPE_IMAGE).equals(mPostHint)) {
                mType = PostType.IMAGE;
            } else if (postTypes.getString(POST_TYPE_HOSTED_VIDEO).equals(mPostHint)) {
                mType = PostType.HOSTED_VIDEO;
            } else if (postTypes.getString(POST_TYPE_RICH_VIDEO).equals(mPostHint)) {
                mType = PostType.RICH_VIDEO;
            }
            if (postTypes.getString(POST_TYPE_SELF).equals(mPostHint)) {
                mType = PostType.SELF;
            }
            postTypes.recycle();
        }

        TypedArray thumbnails =
                context.getResources().obtainTypedArray(R.array.postThumbnailImageTypes);
        if (thumbnails.getString(DEFAULT).equals(mPostThumbnailUrl)) {
            mPostThumbnailType = PostThumbnailType.DEFAULT;
        } else if (thumbnails.getString(SELF).equals(mPostThumbnailUrl)) {
            mPostThumbnailType = PostThumbnailType.SELF;
        } else if (thumbnails.getString(IMAGE).equals(mPostThumbnailUrl)) {
            mPostThumbnailType = PostThumbnailType.IMAGE;
        } else if (!TextUtils.isEmpty(mPostThumbnailUrl)) {
            mPostThumbnailType = PostThumbnailType.THUMBNAIL;
        }
        thumbnails.recycle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(mDate);
        parcel.writeInt(mVotesCount);
        parcel.writeInt(mCommentesCount);
        parcel.writeString(mType == null ? PostType.NO_MEDIA.name() : mType.name());
        parcel.writeString(mPostThumbnailType.name());
        parcel.writeString(mPostId);
        parcel.writeString(mTitle);
        parcel.writeString(mAuthor);
        parcel.writeString(mSubreddit);
        parcel.writeString(mUrl);
        parcel.writeString(mPostDomain);
        parcel.writeString(mPostThumbnailUrl);
        parcel.writeString(mPostHint);
        parcel.writeString(mVideoUrl);
        parcel.writeString(mImageUrl);
        parcel.writeString(mBody);
        parcel.writeString(mSelfText);
        parcel.writeByte((byte) (mVisited ? 1 : 0));
        parcel.writeByte((byte) (mIsFavorite ? 1 : 0));
    }

    public PostType getType() {
        return mType;
    }

    public long getDate() {
        return mDate;
    }

    public int getVotesCount() {
        return mVotesCount;
    }

    public int getCommentsCount() {
        return mCommentesCount;
    }

    public String getPostId() {
        return mPostId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getSubreddit() {
        return mSubreddit;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getDomain() {
        return mPostDomain;
    }

    public String getThumbnailUrl() {
        return mPostThumbnailUrl;
    }

    public String getPostHint() {
        return mPostHint;
    }

    public String getVideoUrl() {
        return mVideoUrl;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getBody() {
        return mBody;
    }

    public String getmSelfText() {
        return mSelfText;
    }

    public boolean isVisited() {
        return mVisited;
    }

    public boolean isFavorite() {
        return mIsFavorite;
    }

    public PostThumbnailType getThumbnailType() {
        return mPostThumbnailType;
    }

    /* Type of the post */
    public enum PostType {
        NO_MEDIA,
        SELF,
        LINK,
        IMAGE,
        HOSTED_VIDEO,
        RICH_VIDEO
    }

    /* Type of the thumnail */
    public enum PostThumbnailType {
        DEFAULT,
        SELF,
        IMAGE,
        THUMBNAIL
    }
}
