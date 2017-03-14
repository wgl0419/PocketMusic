package com.example.q.pocketmusic.module.home.ask.comment;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.example.q.pocketmusic.callback.IBaseView;
import com.example.q.pocketmusic.callback.ToastQueryListListener;
import com.example.q.pocketmusic.callback.ToastQueryListener;
import com.example.q.pocketmusic.callback.ToastSaveListener;
import com.example.q.pocketmusic.callback.ToastUpdateListener;
import com.example.q.pocketmusic.config.CommonString;
import com.example.q.pocketmusic.config.Constant;
import com.example.q.pocketmusic.model.bean.MyUser;
import com.example.q.pocketmusic.model.bean.Song;
import com.example.q.pocketmusic.model.bean.SongObject;
import com.example.q.pocketmusic.model.bean.ask.AskSongComment;
import com.example.q.pocketmusic.model.bean.ask.AskSongPic;
import com.example.q.pocketmusic.model.bean.ask.AskSongPost;
import com.example.q.pocketmusic.module.common.BasePresenter;
import com.example.q.pocketmusic.module.song.SongActivity;
import com.example.q.pocketmusic.util.MyToast;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.listener.UploadBatchListener;
import cn.finalteam.galleryfinal.FunctionConfig;
import cn.finalteam.galleryfinal.GalleryFinal;
import cn.finalteam.galleryfinal.model.PhotoInfo;

/**
 * Created by Cloud on 2016/11/14.
 */

public class AskSongCommentPresenter extends BasePresenter {
    private IView activity;
    private Context context;
    private AskSongPost post;
    private MyUser user;
    private List<String> picUrls = new ArrayList<>();

    public AskSongCommentPresenter(IView activity, Context context, MyUser user, AskSongPost post) {
        this.activity = activity;
        this.context = context;
        this.post = post;
        this.user = user;
    }

    //获得发帖人
    public AskSongPost getPost() {
        return post;
    }

    public void getCommentList() {
        BmobQuery<AskSongComment> queryComment = new BmobQuery<>();
        queryComment.order("-createdAt");
        queryComment.addWhereEqualTo("post", new BmobPointer(post));//对应帖子
        queryComment.include("user,post.user");//发帖人，和评论的信息都找出来
        queryComment.findObjects(new ToastQueryListener<AskSongComment>(context, activity) {
            @Override
            public void onSuccess(List<AskSongComment> list) {
                activity.setCommentList(list);
            }
        });
    }

    //将得到的评论列表进行排序,有图的放在最前面
    //这里最好用list的比较接口实现
    private List<AskSongComment> sort(List<AskSongComment> list) {
        for (int i = 0; i < list.size(); i++) {
            AskSongComment comment = list.get(i);
            if (comment.getHasPic()) {
                list.remove(comment);
                list.add(0, comment);
            }
        }
        return list;
    }

    //发送评论
    public void sendComment(final String comment) {
        if (TextUtils.isEmpty(comment)) {
            return;
        }
        Boolean hasPic;
        hasPic = picUrls.size() > 0;
        final AskSongComment askSongComment = new AskSongComment(user, post, comment, hasPic);
        activity.showLoading(true);
        activity.setCommentInput("");//空
        //添加评论表记录
        askSongComment.save(new ToastSaveListener<String>(context, activity) {
            @Override
            public void onSuccess(final String s) {
                if (picUrls.size() <= 0) {//无图
                    activity.showLoading(false);
                    activity.sendCommentResult(s, askSongComment);
                    return;
                }
                //批量上传图片
                BmobFile.uploadBatch(picUrls.toArray(new String[picUrls.size()]), new UploadBatchListener() {
                    @Override
                    public void onSuccess(final List<BmobFile> list, List<String> list1) {
                        if (picUrls.size() == list1.size()) {// 全部的图片上传成功后调用
                            List<BmobObject> askSongPics = new ArrayList<>();
                            for (int i = 0; i < list.size(); i++) {
                                AskSongPic askSongPic = new AskSongPic(askSongComment, list1.get(i));
                                askSongPics.add(askSongPic);
                            }
                            //批量添加AskSongPic表
                            new BmobBatch().insertBatch(askSongPics).doBatch(new ToastQueryListListener<BatchResult>(context, activity) {
                                @Override
                                public void onSuccess(List<BatchResult> list) {
                                    user.increment("contribution", Constant.ADD_CONTRIBUTION_COMMENT_WITH_PIC); //原子操作
                                    user.update(new ToastUpdateListener(context, activity) {
                                        @Override
                                        public void onSuccess() {
                                            activity.showLoading(false);
                                            MyToast.showToast(context, CommonString.ADD_CONTRIBUTION_BASE + (Constant.ADD_CONTRIBUTION_COMMENT_WITH_PIC));
                                            activity.sendCommentResult(s, askSongComment);
                                        }
                                    });
                                }
                            });
                        }
                    }

                    @Override
                    public void onProgress(int i, int i1, int i2, int i3) {

                    }

                    @Override
                    public void onError(int i, String s) {
                        //文件上传失败
                        activity.showLoading(false);
                        MyToast.showToast(context, CommonString.STR_ERROR_INFO+s);
                    }
                });
            }
        });
    }


    //添加图片
    public void addPic() {
        FunctionConfig config = new FunctionConfig.Builder()
                .setMutiSelectMaxSize(8)
                .build();
        GalleryFinal.openGalleryMuti(3, config, new GalleryFinal.OnHanlderResultCallback() {
            @Override
            public void onHanlderSuccess(int reqeustCode, List<PhotoInfo> resultList) {
                picUrls.clear();
                for (PhotoInfo photoInfo : resultList) {
                    picUrls.add(photoInfo.getPhotoPath());
                }
                activity.addPicResult(picUrls);
            }

            @Override
            public void onHanlderFailure(int requestCode, String errorMsg) {
                MyToast.showToast(context, CommonString.STR_ERROR_INFO + errorMsg);
            }
        });
    }

    //得到所有图片，显示dialog
    public void alertPicDialog(final AskSongComment askSongComment) {
        if (askSongComment.getHasPic()) {
            activity.showLoading(true);
            //查询有多少张图片
            BmobQuery<AskSongPic> query = new BmobQuery<>();
            query.addWhereEqualTo("comment", new BmobPointer(askSongComment));
            query.findObjects(new ToastQueryListener<AskSongPic>(context, activity) {
                @Override
                public void onSuccess(List<AskSongPic> list) {
                    final Song song = new Song(askSongComment.getContent(), null);//将评论者的内容当做标题
                    song.setDate(askSongComment.getCreatedAt());
                    List<String> urls = new ArrayList<String>();
                    for (AskSongPic askSongPic : list) {
                        urls.add(askSongPic.getUrl());
                    }
                    song.setIvUrl(urls);
                    activity.showLoading(false);
                    activity.showPicDialog(song, askSongComment);

                }
            });
        }

    }

    //进入SongActivity
    public void enterSongActivity(Song song, AskSongComment askSongComment) {
        Intent intent = new Intent(context, SongActivity.class);
        song.setNeedGrade(true);//收费
        SongObject songObject = new SongObject(song, Constant.FROM_ASK, Constant.SHOW_ALL_MENU, Constant.NET);
        intent.putExtra(SongActivity.PARAM_SONG_OBJECT_PARCEL, songObject);
        intent.putExtra(SongActivity.ASK_COMMENT, askSongComment);
        context.startActivity(intent);
    }


    public interface IView extends IBaseView {

        void setCommentList(List<AskSongComment> list);

        void sendCommentResult(String s, AskSongComment askSongComment);

        void addPicResult(List<String> picUrls);

        void setCommentInput(String s);

        void showPicDialog(Song song, AskSongComment askSongComment);
    }
}
