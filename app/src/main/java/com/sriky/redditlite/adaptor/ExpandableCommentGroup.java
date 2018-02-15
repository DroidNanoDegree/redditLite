package com.sriky.redditlite.adaptor;

import com.xwray.groupie.ExpandableGroup;

import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.List;


/**
 * Original source can be found here:
 * https://github.com/PedroCarrillo/RedditApp/tree/feature/comments
 */

public class ExpandableCommentGroup extends ExpandableGroup {
    public ExpandableCommentGroup(CommentNode<Comment> commentNode, int depth) {
        super(new ExpandableCommentItem(commentNode, depth));

        if (commentNode.hasMoreChildren()) {
            List<CommentNode<Comment>> replies = commentNode.getReplies();
            for (CommentNode<Comment> commentChildNode : replies) {
                this.add(new ExpandableCommentGroup(commentChildNode, (depth + 1)));
            }
        }
    }
}
