package com.xaveco.videocutter;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import java.io.File;
import java.nio.ByteBuffer;

final class VideoTrimmer {
    static void trim(Context context,Uri source,File destination,long durationUs) throws Exception {
        MediaExtractor extractor=new MediaExtractor(); MediaMuxer muxer=null; boolean started=false;
        try {
            extractor.setDataSource(context,source,null);
            int count=extractor.getTrackCount(); int[] map=new int[count]; boolean hasVideo=false;
            for(int i=0;i<count;i++) {
                MediaFormat f=extractor.getTrackFormat(i); String mime=f.getString(MediaFormat.KEY_MIME);
                if(mime!=null&&(mime.startsWith("video/")||mime.startsWith("audio/"))) { extractor.selectTrack(i); map[i]=-1; if(mime.startsWith("video/")) hasVideo=true; }
                else map[i]=-2;
            }
            if(!hasVideo) throw new IllegalArgumentException("O arquivo não contém vídeo.");
            muxer=new MediaMuxer(destination.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            for(int i=0;i<count;i++) if(map[i]==-1) muxer.addTrack(extractor.getTrackFormat(i));
            int outIndex=0; for(int i=0;i<count;i++) if(map[i]==-1) map[i]=outIndex++;
            for(int i=0;i<count;i++) if(map[i]>=0) {
                MediaFormat f=extractor.getTrackFormat(i); String mime=f.getString(MediaFormat.KEY_MIME);
                if(mime!=null&&mime.startsWith("video/")&&f.containsKey(MediaFormat.KEY_ROTATION)) { try { muxer.setOrientationHint(f.getInteger(MediaFormat.KEY_ROTATION)); } catch(Exception ignored) {} break; }
            }
            muxer.start(); started=true;
            ByteBuffer buffer=ByteBuffer.allocate(4*1024*1024); MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while(true) {
                int track=extractor.getSampleTrackIndex(); if(track<0) break;
                long time=extractor.getSampleTime(); if(time<0||time>=durationUs) break;
                if(map[track]>=0) {
                    buffer.clear(); int size=extractor.readSampleData(buffer,0);
                    if(size>0) { info.offset=0; info.size=size; info.presentationTimeUs=time; info.flags=extractor.getSampleFlags(); muxer.writeSampleData(map[track],buffer,info); }
                }
                if(!extractor.advance()) break;
            }
        } finally {
            if(muxer!=null) { if(started) try { muxer.stop(); } catch(Exception ignored) {} muxer.release(); }
            extractor.release();
        }
    }

    static void join(java.util.List<File> clips, File destination) throws Exception {
        if (clips == null || clips.isEmpty()) throw new IllegalArgumentException("Nenhum trecho para unir.");
        MediaExtractor first = new MediaExtractor();
        MediaMuxer muxer = null;
        boolean started = false;
        try {
            first.setDataSource(clips.get(0).getAbsolutePath());
            int count = first.getTrackCount();
            int[] outTracks = new int[count];
            String[] mimeTypes = new String[count];
            for (int i = 0; i < count; i++) {
                MediaFormat fmt = first.getTrackFormat(i);
                mimeTypes[i] = fmt.getString(MediaFormat.KEY_MIME);
                outTracks[i] = i;
            }
            muxer = new MediaMuxer(destination.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            for (int i = 0; i < count; i++) muxer.addTrack(first.getTrackFormat(i));
            muxer.start(); started = true;
            long timelineOffsetUs = 0;
            ByteBuffer buffer = ByteBuffer.allocate(4 * 1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            for (File clip : clips) {
                MediaExtractor ex = new MediaExtractor();
                try {
                    ex.setDataSource(clip.getAbsolutePath());
                    if (ex.getTrackCount() != count) throw new IllegalArgumentException("Os vídeos precisam ter as mesmas faixas de áudio/vídeo.");
                    long maxEndUs = 0;
                    for (int i = 0; i < count; i++) {
                        String actual = ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
                        if (actual == null || !actual.equals(mimeTypes[i]))
                            throw new IllegalArgumentException("Formato de vídeo/áudio incompatível entre os arquivos.");
                        ex.selectTrack(i);
                    }
                    while (true) {
                        int track = ex.getSampleTrackIndex();
                        if (track < 0) break;
                        long time = ex.getSampleTime();
                        if (time < 0) break;
                        buffer.clear();
                        int size = ex.readSampleData(buffer, 0);
                        if (size > 0) {
                            info.offset = 0;
                            info.size = size;
                            info.presentationTimeUs = timelineOffsetUs + time;
                            info.flags = ex.getSampleFlags();
                            muxer.writeSampleData(outTracks[track], buffer, info);
                            if (time > maxEndUs) maxEndUs = time;
                        }
                        if (!ex.advance()) break;
                    }
                    timelineOffsetUs += maxEndUs + 40_000L;
                } finally { ex.release(); }
            }
        } finally {
            first.release();
            if (muxer != null) {
                if (started) try { muxer.stop(); } catch (Exception ignored) {}
                muxer.release();
            }
        }
    }

}
