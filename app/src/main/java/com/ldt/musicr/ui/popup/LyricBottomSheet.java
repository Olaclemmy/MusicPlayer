package com.ldt.musicr.ui.popup;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.widget.NestedScrollView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ldt.musicr.R;
import com.ldt.musicr.model.Song;
import com.ldt.musicr.model.lyrics.Lyrics;
import com.ldt.musicr.service.MusicPlayerRemote;
import com.ldt.musicr.service.MusicServiceEventListener;
import com.ldt.musicr.ui.BaseActivity;
import com.ldt.musicr.util.MusicUtil;
import com.ldt.musicr.util.Tool;
import com.squareup.picasso.Picasso;

import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED;

public class LyricBottomSheet extends BottomSheetDialogFragment  implements MusicServiceEventListener {
    private static final String TAG = "LyricBottomSheet";
    private static final String SONG_KEY = "song";
    private static final String SHOULD_AUTO_UPDATE_KEY = "should_auto_update";

    @BindView(R.id.lyric_content)
    TextView mLyricContent;
    @BindView(R.id.root) View mRoot;

    @BindView(R.id.nested_scroll_view)
    NestedScrollView mScrollView;

    @BindView(R.id.align_view) View mAlignView;

    @OnClick({R.id.back,R.id.parent})
    void doDismiss() {
        dismiss();
    }

    public static LyricBottomSheet newInstance(Song song) {

        Bundle args = new Bundle();
        args.putParcelable(SONG_KEY, song);
        args.putBoolean(SHOULD_AUTO_UPDATE_KEY,false);

        LyricBottomSheet fragment = new LyricBottomSheet();
        fragment.setArguments(args);

        return fragment;
    }
    private boolean mShouldAutoUpdate = false;

/*    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }*/

    public static LyricBottomSheet newInstance() {

        Bundle args = new Bundle();
        args.putParcelable(SONG_KEY, MusicPlayerRemote.getCurrentSong());
        args.putBoolean(SHOULD_AUTO_UPDATE_KEY,true);
        LyricBottomSheet fragment = new LyricBottomSheet();
        fragment.setArguments(args);
        return fragment;
    }
    Song mSong;

/*    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getView() !=null) {
            ViewParent v = getView().getParent();
            if (v instanceof View) {
                ((View) v).setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }*/
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.TransparentBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lyric_bottom_sheet,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);
        ButterKnife.bind(this,view);
        mAlignView.getLayoutParams ().height = (int) (Tool.getScreenSize(getContext())[1]);
        mAlignView.requestLayout();

        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            FrameLayout bottomSheet = (FrameLayout)
                    dialog.findViewById(android.support.design.R.id.design_bottom_sheet);
            BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setPeekHeight(-Tool.getNavigationHeight(requireActivity()));
            behavior.setHideable(false);
            behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if(newState==STATE_COLLAPSED)
                        LyricBottomSheet.this.dismiss();
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                }
            });
        });

        // onViewCreated();
        Bundle bundle = getArguments();
        if(bundle !=null) {
            mSong = bundle.getParcelable(SONG_KEY);
            mShouldAutoUpdate = bundle.getBoolean(SHOULD_AUTO_UPDATE_KEY,false);
        }
        updateLyric();
        if(getActivity() instanceof BaseActivity)
            ((BaseActivity)getActivity()).addMusicServiceEventListener(this);
    }

    @Override
    public void onDestroyView() {
        if(getActivity() instanceof BaseActivity)
            ((BaseActivity)getActivity()).removeMusicServiceEventListener(this);
        super.onDestroyView();
    }

    @BindView(R.id.title) TextView mTitle;
    @BindView(R.id.artist) TextView mArtist;
    @BindView(R.id.image)
    ImageView mImageView;
    private void updateLyric() {
        if(mSong !=null) {
           String lyric = MusicUtil.getLyrics(mSong);
           if(lyric==null||lyric.isEmpty()) lyric = "This song has no lyric.";

           boolean isHtml = isHtml(lyric);
           if(isHtml) {
               Spanned spanned = Html.fromHtml(lyric);
               mLyricContent.setText(spanned);
           } else
           mLyricContent.setText(lyric);

            Log.d(TAG, "updateLyric: "+lyric);
            mTitle.setText(mSong.title);
            mArtist.setText(mSong.artistName);
            if(getContext() !=null)
            Glide.with(getContext()).load(MusicUtil.getMediaStoreAlbumCoverUri(mSong.albumId)).placeholder(R.drawable.music_style).error(R.drawable.music_style).into(mImageView);
            else Picasso.get().load(MusicUtil.getMediaStoreAlbumCoverUri(mSong.albumId)).placeholder(R.drawable.music_style).error(R.drawable.music_style).into(mImageView);
        }
        else Log.d(TAG, "updateLyric: Song is null");
    }
    private void setClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
    }

    @OnClick(R.id.copy)
    void copy() {
        if(getContext() !=null) {
            setClipboard(getContext(), MusicUtil.getLyrics(mSong));
            Toast.makeText(getContext(),"Copied",Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.edit)
    void edit() {

    }
    // adapted from re posted by Phil Haack and modified to match better
    public final static String tagStart=
            "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>";
    public final static String tagEnd=
            "\\</\\w+\\>";
    public final static String tagSelfClosing=
            "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>";
    public final static String htmlEntity=
            "&[a-zA-Z][a-zA-Z0-9]+;";
    public final static Pattern htmlPattern=Pattern.compile(
            "("+tagStart+".*"+tagEnd+")|("+tagSelfClosing+")|("+htmlEntity+")",
            Pattern.DOTALL
    );
    public static boolean isHtml(String s) {
        boolean ret=false;
        if (s != null) {
            ret=htmlPattern.matcher(s).find();
        }
        return ret;
    }

    @Override
    public void onServiceConnected() {
        if(mShouldAutoUpdate) autoUpdateLyric();
    }

    @Override
    public void onServiceDisconnected() {

    }

    @Override
    public void onQueueChanged() {

    }

    @Override
    public void onPlayingMetaChanged() {
        if(mShouldAutoUpdate) autoUpdateLyric();
    }
    public void autoUpdateLyric() {
        mSong = MusicPlayerRemote.getCurrentSong();
        updateLyric();
    }

    @Override
    public void onPlayStateChanged() {

    }

    @Override
    public void onRepeatModeChanged() {

    }

    @Override
    public void onShuffleModeChanged() {

    }

    @Override
    public void onMediaStoreChanged() {

    }
}
