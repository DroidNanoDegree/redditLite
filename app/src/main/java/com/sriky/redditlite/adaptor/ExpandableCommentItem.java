package com.sriky.redditlite.adaptor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sriky.redditlite.R;
import com.sriky.redditlite.utils.RedditLiteUtils;
import com.xwray.groupie.ExpandableGroup;
import com.xwray.groupie.ExpandableItem;
import com.xwray.groupie.Item;
import com.xwray.groupie.ViewHolder;

import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.Locale;

import timber.log.Timber;

/**
 * Original source can be found here:
 * https://github.com/PedroCarrillo/RedditApp/tree/feature/comments
 */

public class ExpandableCommentItem extends Item implements ExpandableItem {
    private ExpandableGroup mExpandableGroup;
    private CommentNode<Comment> mCommentNode;
    private int mDepth;
    private boolean mExpanded;

    public ExpandableCommentItem(CommentNode<Comment> comment, int depth) {
        super();
        mCommentNode = comment;
        mDepth = depth;
    }

    @Override
    public void setExpandableGroup(@NonNull ExpandableGroup onToggleListener) {
        mExpandableGroup = onToggleListener;
    }

    @Override
    public void bind(@NonNull ViewHolder viewHolder, int position) {

        indentCommentReplies(viewHolder);
        Context context = viewHolder.itemView.getContext();
        Comment comment = mCommentNode.getSubject();

        //set the username
        String authorName = context.getString(R.string.comment_author_format);
        String formattedAuthorName = String.format(authorName, comment.getAuthor());
        TextView author = viewHolder.itemView.findViewById(R.id.tv_user);
        author.setText(formattedAuthorName);

        //set date
        TextView date = viewHolder.itemView.findViewById(R.id.comment_date);
        date.setText(RedditLiteUtils.getFormattedDateFromNow(context, comment.getCreated().getTime()));

        //set votes
        Button btn = viewHolder.itemView.findViewById(R.id.btn_votes_count);
        btn.setText(RedditLiteUtils.getFormattedCountByThousand(context, comment.getScore()));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vote();
            }
        });

        //set the comment.
        TextView body = viewHolder.itemView.findViewById(R.id.comment_body);
        //support launching urls from comments.
        Linkify.addLinks(body, Linkify.WEB_URLS);
        body.setText(comment.getBody());

        final Button toggle = viewHolder.itemView.findViewById(R.id.toggle_expand);
        toggle.setText(String.format(Locale.getDefault(), "%d %s",
                mCommentNode.totalSize(), context.getString(R.string.replies)));

        if (mCommentNode.totalSize() > 0) {
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mExpanded = !mExpanded;
                    if (mExpanded) {
                        toggle.setCompoundDrawablesWithIntrinsicBounds(0,
                                0, R.drawable.ic_expand_less, 0);
                    } else {
                        toggle.setCompoundDrawablesWithIntrinsicBounds(0,
                                0, R.drawable.ic_expand_more, 0);
                    }
                    mExpandableGroup.onToggleExpanded();
                }
            });
        } else {
            //hide the toggle btn.
            toggle.setCompoundDrawablesWithIntrinsicBounds(0,
                    0, 0, 0);
        }
    }

    @Override
    public int getLayout() {
        return R.layout.expandable_comment_item;
    }

    /**
     * Adds the indentation for comment replies.
     *
     * @param viewHolder The ViewHolder containing the View.
     */
    private void indentCommentReplies(ViewHolder viewHolder) {
        //add the indentation for comment replies.
        LinearLayout separatorContainer = viewHolder.itemView.findViewById(R.id.separatorContainer);
        separatorContainer.removeAllViews();
        separatorContainer.setVisibility(mDepth > 0 ? View.VISIBLE : View.GONE);
        for (int i = 1; i <= mDepth; i++) {
            View line = LayoutInflater.from(viewHolder.itemView.getContext())
                    .inflate(R.layout.expandable_comment_item_separator, separatorContainer, false);

            separatorContainer.addView(line);
        }

        //remove the top margin for child items.
        if (mDepth > 0) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewHolder.itemView.getLayoutParams();
            params.setMargins(params.leftMargin,
                    0,
                    params.rightMargin,
                    params.bottomMargin);
        }

        //request layout to re-draw/calculate the layout.
        TextView body = viewHolder.itemView.findViewById(R.id.comment_body);
        body.requestLayout();
    }

    private void vote() {
        //TODO: implement login in the utils class and use it in main activity as well.
        Timber.e("TODO!!!");
    }
}
