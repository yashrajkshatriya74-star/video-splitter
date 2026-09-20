package com.example.videosplitter;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int PICK_VIDEO = 1001;
    private TextView selected, durationText, status;
    private EditText durationInput, partsInput;
    private Spinner modeSpinner;
    private Button splitButton;
    private Uri videoUri;
    private long videoDurationUs = 0;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(14,16,20));
        getWindow().setNavigationBarColor(Color.rgb(14,16,20));
        buildUi();
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 7);
    }

    private TextView tv(String s, float sp, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setPadding(0,0,0,0); return t;
    }
    private GradientDrawable bg(int color, float radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(radius); return g; }
    private int dp(float x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }

    private void buildUi() {
        int white=Color.rgb(245,245,247), muted=Color.rgb(154,160,170), surface=Color.rgb(23,26,33), surface2=Color.rgb(32,36,45), accent=Color.rgb(124,92,255);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(20),dp(20),dp(16)); root.setBackgroundColor(Color.rgb(14,16,20));
        TextView title=tv("Video Splitter",28,white); title.setTypeface(null,1); root.addView(title,new LinearLayout.LayoutParams(-1,dp(44)));
        TextView sub=tv("Fast offline splitting for MP4, MOV and other Android-supported videos",14,muted); sub.setPadding(0,0,0,dp(18)); root.addView(sub,new LinearLayout.LayoutParams(-1,dp(48)));

        LinearLayout pick=new LinearLayout(this); pick.setOrientation(LinearLayout.HORIZONTAL); pick.setGravity(Gravity.CENTER_VERTICAL); pick.setPadding(dp(14),dp(12),dp(14),dp(12)); pick.setBackground(bg(surface,dp(18)));
        ImageView icon=new ImageView(this); icon.setImageResource(com.example.videosplitter.R.drawable.ic_video); pick.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(dp(12),0,dp(8),0); selected=tv("No video selected",16,white); durationText=tv("Tap to choose a video",13,muted); names.addView(selected); names.addView(durationText); pick.addView(names,new LinearLayout.LayoutParams(0,dp(58),1));
        pick.setOnClickListener(v->pickVideo()); root.addView(pick,new LinearLayout.LayoutParams(-1,dp(82)));

        Space sp1=new Space(this); root.addView(sp1,new LinearLayout.LayoutParams(1,dp(18)));
        root.addView(tv("SPLIT MODE",12,muted),new LinearLayout.LayoutParams(-1,dp(24)));
        modeSpinner=new Spinner(this); String[] modes={"Fixed duration per clip","Equal number of parts"}; modeSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,modes)); modeSpinner.setBackground(bg(surface2,dp(14))); root.addView(modeSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        Space sp2=new Space(this); root.addView(sp2,new LinearLayout.LayoutParams(1,dp(12)));

        durationInput=new EditText(this); durationInput.setHint("Seconds per clip (e.g. 15)"); durationInput.setHintTextColor(muted); durationInput.setTextColor(white); durationInput.setTextSize(16); durationInput.setSingleLine(); durationInput.setInputType(2|8192); durationInput.setPadding(dp(16),0,dp(16),0); durationInput.setBackground(bg(surface2,dp(14))); root.addView(durationInput,new LinearLayout.LayoutParams(-1,dp(52)));
        partsInput=new EditText(this); partsInput.setHint("Number of parts (e.g. 4)"); partsInput.setHintTextColor(muted); partsInput.setTextColor(white); partsInput.setTextSize(16); partsInput.setSingleLine(); partsInput.setInputType(2); partsInput.setPadding(dp(16),0,dp(16),0); partsInput.setBackground(bg(surface2,dp(14))); partsInput.setVisibility(View.GONE); root.addView(partsInput,new LinearLayout.LayoutParams(-1,dp(52)));
        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){durationInput.setVisibility(pos==0?View.VISIBLE:View.GONE); partsInput.setVisibility(pos==1?View.VISIBLE:View.GONE);}});
        Space sp3=new Space(this); root.addView(sp3,new LinearLayout.LayoutParams(1,dp(12)));

        splitButton=new Button(this); splitButton.setText("SPLIT VIDEO"); splitButton.setTextColor(Color.WHITE); splitButton.setTextSize(15); splitButton.setTypeface(null,1); splitButton.setAllCaps(false); splitButton.setBackground(bg(accent,dp(16))); splitButton.setOnClickListener(v->startSplit()); root.addView(splitButton,new LinearLayout.LayoutParams(-1,dp(56)));
        Space sp4=new Space(this); root.addView(sp4,new LinearLayout.LayoutParams(1,dp(14)));
        status=tv("Output: Movies/VideoSplitter",13,muted); status.setGravity(Gravity.CENTER); root.addView(status,new LinearLayout.LayoutParams(-1,dp(40)));
        TextView note=tv("No re-encoding • Original quality preserved • Splits may align to keyframes",12,muted); note.setGravity(Gravity.CENTER); root.addView(note,new LinearLayout.LayoutParams(-1,dp(42)));
        setContentView(root);
    }

    private void pickVideo(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*"); startActivityForResult(i,PICK_VIDEO);
    }
    @Override protected void onActivityResult(int req,int result,Intent data){ super.onActivityResult(req,result,data); if(req==PICK_VIDEO&&result==RESULT_OK&&data!=null){ videoUri=data.getData(); try{ getContentResolver().takePersistableUriPermission(videoUri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); }catch(Exception ignored){} readMetadata(); }}
    private void readMetadata(){
        try{
            MediaMetadataRetriever r=new MediaMetadataRetriever(); r.setDataSource(this,videoUri); String d=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION); videoDurationUs=Long.parseLong(d)*1000L; r.release();
            String name=getDisplayName(videoUri); selected.setText(name); durationText.setText(formatUs(videoDurationUs)+" • ready to split");
        }catch(Exception e){ Toast.makeText(this,"Could not read this video",Toast.LENGTH_LONG).show(); }
    }
    private String getDisplayName(Uri u){ Cursor c=getContentResolver().query(u,new String[]{MediaStore.MediaColumns.DISPLAY_NAME},null,null,null); if(c!=null){try{if(c.moveToFirst())return c.getString(0);}finally{c.close();}} return "Selected video"; }
    private String formatUs(long us){ long sec=us/1000000; return String.format(Locale.US,"%02d:%02d:%02d",sec/3600,(sec/60)%60,sec%60); }

    private void startSplit(){
        if(videoUri==null){Toast.makeText(this,"Choose a video first",Toast.LENGTH_SHORT).show();return;}
        final boolean equal=modeSpinner.getSelectedItemPosition()==1; final long chunkUs;
        try{
            if(equal){ int n=Integer.parseInt(partsInput.getText().toString().trim()); if(n<2||n>999)throw new Exception(); chunkUs=(long)Math.ceil(videoDurationUs/(double)n); }
            else { double sec=Double.parseDouble(durationInput.getText().toString().trim()); if(sec<=0||sec>86400)throw new Exception(); chunkUs=(long)(sec*1000000L); }
        }catch(Exception e){Toast.makeText(this,"Enter a valid split value",Toast.LENGTH_SHORT).show();return;}
        splitButton.setEnabled(false); status.setText("Splitting… keep the app open");
        executor.submit(()->{
            try{ int count=splitVideo(videoUri,chunkUs); runOnUiThread(()->{splitButton.setEnabled(true);status.setText("Done • "+count+" clips saved to Movies/VideoSplitter");Toast.makeText(this,"Finished: "+count+" clips",Toast.LENGTH_LONG).show();}); }
            catch(Exception e){ final String msg=e.getMessage()==null?"Unknown error":e.getMessage(); runOnUiThread(()->{splitButton.setEnabled(true);status.setText("Split failed");Toast.makeText(this,"Split failed: "+msg,Toast.LENGTH_LONG).show();}); }
        });
    }

    private int splitVideo(Uri uri,long chunkUs) throws Exception {
        MediaMetadataRetriever mmr=new MediaMetadataRetriever(); mmr.setDataSource(this,uri); long total=Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))*1000L; String rot=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION); mmr.release();
        int parts=(int)((total+chunkUs-1)/chunkUs), made=0;
        for(int p=0;p<parts;p++){
            long requestedStart=p*chunkUs, requestedEnd=Math.min(total,(p+1)*chunkUs); String base=stripExt(getDisplayName(uri)); String filename=base+"_part_"+String.format(Locale.US,"%03d",p+1)+".mp4";
            OutputTarget out=createOutput(filename); MediaMuxer mux=out.muxer; boolean started=false;
            try{
                ArrayList<Integer> tracks=new ArrayList<>();
                MediaExtractor probe=new MediaExtractor(); probe.setDataSource(this,uri,null);
                for(int i=0;i<probe.getTrackCount();i++){MediaFormat f=probe.getTrackFormat(i); String mime=f.getString(MediaFormat.KEY_MIME); if(mime!=null&&(mime.startsWith("video/")||mime.startsWith("audio/"))){tracks.add(i); mux.addTrack(f);}}
                probe.release(); if(tracks.isEmpty())throw new IOException("No audio/video track found");
                if(rot!=null)try{mux.setOrientationHint(Integer.parseInt(rot));}catch(Exception ignored){}
                mux.start(); started=true;
                for(int trackIndex:tracks){
                    MediaExtractor ex=new MediaExtractor(); ex.setDataSource(this,uri,null); ex.selectTrack(trackIndex); ex.seekTo(requestedStart,MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    MediaFormat fmt=ex.getTrackFormat(trackIndex); int max=fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)?fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE):1024*1024; ByteBuffer buf=ByteBuffer.allocateDirect(Math.max(max,1024*1024)); MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
                    while(true){ long t=ex.getSampleTime(); if(t<0||t>=requestedEnd)break; int size=ex.readSampleData(buf,0); if(size<0)break; info.offset=0; info.size=size; info.presentationTimeUs=t-requestedStart; info.flags=ex.getSampleFlags(); mux.writeSampleData(tracks.indexOf(trackIndex),buf,info); ex.advance(); }
                    ex.release();
                }
            } finally { if(started)try{mux.stop();}catch(Exception ignored){} try{mux.release();}catch(Exception ignored){} }
            out.publish(); made++;
        }
        return made;
    }

    private String stripExt(String s){int i=s.lastIndexOf('.');return i>0?s.substring(0,i):s;}
    private static class OutputTarget { MediaMuxer muxer; ParcelFileDescriptor pfd; File file; Runnable publish; void publish(){if(pfd!=null)try{pfd.close();}catch(Exception ignored){} if(publish!=null)publish.run();} }
    private OutputTarget createOutput(String filename) throws Exception {
        OutputTarget o=new OutputTarget();
        if(Build.VERSION.SDK_INT>=29){
            ContentValues v=new ContentValues(); v.put(MediaStore.Video.Media.DISPLAY_NAME,filename); v.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4"); v.put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/VideoSplitter"); v.put(MediaStore.Video.Media.IS_PENDING,1); Uri u=getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,v); if(u==null)throw new IOException("Could not create output"); o.pfd=getContentResolver().openFileDescriptor(u,"w"); o.muxer=new MediaMuxer(o.pfd.getFileDescriptor(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4); o.publish=()->{ContentValues done=new ContentValues();done.put(MediaStore.Video.Media.IS_PENDING,0);getContentResolver().update(u,done,null,null);};
        } else { File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),"VideoSplitter"); if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create output folder"); o.file=new File(dir,filename); o.muxer=new MediaMuxer(o.file.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4); o.publish=()->{}; }
        return o;
    }
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
