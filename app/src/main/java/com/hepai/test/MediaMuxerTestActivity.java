package com.hepai.test;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class MediaMuxerTestActivity extends AppCompatActivity {

    private static final String TAG = "MediaMuxerTest";
    private static final boolean VERBOSE = true;
    private static final int MAX_SAMPLE_SIZE = 256 * 1024;
    private Resources mResources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResources = getResources();
        try {
            testVideoAudio();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test: make sure the muxer handles both video and audio tracks correctly.
     */
    public void testVideoAudio() throws Exception {
        int source = R.raw.video_176x144_3gp_h263_300kbps_25fps_aac_stereo_128kbps_11025hz;
        String outputFile = Environment.getExternalStorageDirectory() + "/videoAudio.mp4";
        cloneAndVerify(source, outputFile, 2, 90);
    }

    /**
     * Test: make sure the muxer handles audio track only file correctly.
     */
    public void testAudioOnly() throws Exception {
        int source = R.raw.sinesweepm4a;
        String outputFile = "/sdcard/audioOnly.mp4";
        cloneAndVerify(source, outputFile, 1, -1);
    }

    /**
     * Test: make sure the muxer handles video track only file correctly.
     */
    public void testVideoOnly() throws Exception {
        int source = R.raw.video_only_176x144_3gp_h263_25fps;
        String outputFile = "/sdcard/videoOnly.mp4";
        cloneAndVerify(source, outputFile, 1, 180);
    }

    /**
     * Tests: make sure the muxer handles exceptions correctly.
     * <br> Throws exception b/c start() is not called.
     * <br> Throws exception b/c 2 video tracks were added.
     * <br> Throws exception b/c 2 audio tracks were added.
     * <br> Throws exception b/c 3 tracks were added.
     * <br> Throws exception b/c no tracks was added.
     * <br> Throws exception b/c a wrong format.
     */
    public void testIllegalStateExceptions() throws IOException {
        String outputFile = "/sdcard/muxerExceptions.mp4";
        MediaMuxer muxer;
        // Throws exception b/c start() is not called.
        muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxer.addTrack(MediaFormat.createVideoFormat("video/avc", 480, 320));
        try {
            muxer.stop();
            Log.i(TAG, "should throw IllegalStateException.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        // Throws exception b/c 2 video tracks were added.
        muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxer.addTrack(MediaFormat.createVideoFormat("video/avc", 480, 320));
        try {
            muxer.addTrack(MediaFormat.createVideoFormat("video/avc", 480, 320));
            Log.i(TAG, "should throw IllegalStateException.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        // Throws exception b/c 2 audio tracks were added.
        muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxer.addTrack(MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 1));
        try {
            muxer.addTrack(MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 1));
            Log.i(TAG, "should throw IllegalStateException.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        // Throws exception b/c 3 tracks were added.
        muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        muxer.addTrack(MediaFormat.createVideoFormat("video/avc", 480, 320));
        muxer.addTrack(MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 1));
        try {
            muxer.addTrack(MediaFormat.createVideoFormat("video/avc", 480, 320));
            Log.i(TAG, "should throw IllegalStateException.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        // Throws exception b/c no tracks was added.
        muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        try {
            muxer.start();
            Log.i(TAG, "should throw IllegalStateException.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        // Throws exception b/c a wrong format.
        muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        try {
            muxer.addTrack(MediaFormat.createVideoFormat("vidoe/mp4", 480, 320));
            Log.i(TAG, "should throw IllegalStateException.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        new File(outputFile).delete();
    }

    /**
     * Using the MediaMuxer to clone a media file.
     */
    private void cloneMediaUsingMuxer(int srcMedia, String dstMediaPath, int expectedTrackCount, int degrees) throws IOException {
        // Set up MediaExtractor to read from the source.
        AssetFileDescriptor srcFd = mResources.openRawResourceFd(srcMedia);
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(srcFd.getFileDescriptor(), srcFd.getStartOffset(), srcFd.getLength());
        int trackCount = extractor.getTrackCount();
        //assertEquals("wrong number of tracks", expectedTrackCount, trackCount);
        // Set up MediaMuxer for the destination.
        MediaMuxer muxer;
        muxer = new MediaMuxer(dstMediaPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        // Set up the tracks.
        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mine = format.getString(MediaFormat.KEY_MIME);
            Log.i(TAG, "mine = " + mine);
            if (mine.startsWith("video/")) {//audio
                int dstIndex = muxer.addTrack(format);
                indexMap.put(i, dstIndex);
                extractor.selectTrack(i);
            }
        }
        // Copy the samples from MediaExtractor to MediaMuxer.
        boolean sawEOS = false;
        int bufferSize = MAX_SAMPLE_SIZE;
        int frameCount = 0;
        int offset = 100;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        if (degrees >= 0) {
            muxer.setOrientationHint(90);
        }
        muxer.start();
        while (!sawEOS) {
            bufferInfo.offset = offset;
            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
            if (bufferInfo.size < 0) {
                if (VERBOSE) {
                    Log.d(TAG, "saw input EOS.");
                }
                sawEOS = true;
                bufferInfo.size = 0;
            } else {
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                bufferInfo.flags = extractor.getSampleFlags();
                int trackIndex = extractor.getSampleTrackIndex();

                muxer.writeSampleData(indexMap.get(trackIndex), dstBuf, bufferInfo);

                extractor.advance();
                frameCount++;
                if (VERBOSE) {
                    Log.d(TAG, "Frame (" + frameCount + ") " +
                            "PresentationTimeUs:" + bufferInfo.presentationTimeUs +
                            " Flags:" + bufferInfo.flags +
                            " TrackIndex:" + trackIndex +
                            " Size(KB) " + bufferInfo.size / 1024);
                }
            }
        }
        muxer.stop();
        muxer.release();
        srcFd.close();
        return;
    }

    /**
     * Clones a media file and then compares against the source file to make
     * sure they match.
     */
    private void cloneAndVerify(int srcMedia, String outputMediaFile, int expectedTrackCount, int degrees) throws IOException {
        try {
            cloneMediaUsingMuxer(srcMedia, outputMediaFile, expectedTrackCount, degrees);
            verifyAttributesMatch(srcMedia, outputMediaFile, degrees);
            // Check the sample on 1s and 0.5s.
            verifySamplesMatch(srcMedia, outputMediaFile, 1000000);
            verifySamplesMatch(srcMedia, outputMediaFile, 500000);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            //new File(outputMediaFile).delete();
        }
    }

    /**
     * Compares some attributes using MediaMetadataRetriever to make sure the
     * cloned media file matches the source file.
     */
    private void verifyAttributesMatch(int srcMedia, String testMediaPath,
                                       int degrees) {
        AssetFileDescriptor testFd = mResources.openRawResourceFd(srcMedia);
        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(testFd.getFileDescriptor(),
                testFd.getStartOffset(), testFd.getLength());
        MediaMetadataRetriever retrieverTest = new MediaMetadataRetriever();
        retrieverTest.setDataSource(testMediaPath);
        String testDegrees = retrieverTest.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (testDegrees != null) {
            //assertEquals("Different degrees", degrees, Integer.parseInt(testDegrees));
        }
        String heightSrc = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String heightTest = retrieverTest.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        //assertEquals("Different height", heightSrc, heightTest);
        String widthSrc = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String widthTest = retrieverTest.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        //assertEquals("Different height", widthSrc, widthTest);
        String durationSrc = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String durationTest = retrieverTest.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        //assertEquals("Different height", durationSrc, durationTest);
        retrieverSrc.release();
        retrieverTest.release();
    }

    /**
     * Uses 2 MediaExtractor, seeking to the same position, reads the sample and
     * makes sure the samples match.
     */
    private void verifySamplesMatch(int srcMedia, String testMediaPath,
                                    int seekToUs) throws IOException {
        AssetFileDescriptor testFd = mResources.openRawResourceFd(srcMedia);
        MediaExtractor extractorSrc = new MediaExtractor();
        extractorSrc.setDataSource(testFd.getFileDescriptor(),
                testFd.getStartOffset(), testFd.getLength());
        int trackCount = extractorSrc.getTrackCount();
        MediaExtractor extractorTest = new MediaExtractor();
        extractorTest.setDataSource(testMediaPath);
        //assertEquals("wrong number of tracks", trackCount, extractorTest.getTrackCount());
        // Make sure the format is the same and select them
        for (int i = 0; i < trackCount; i++) {
            MediaFormat formatSrc = extractorSrc.getTrackFormat(i);
            MediaFormat formatTest = extractorTest.getTrackFormat(i);
            String mimeIn = formatSrc.getString(MediaFormat.KEY_MIME);
            String mimeOut = formatTest.getString(MediaFormat.KEY_MIME);
            if (!(mimeIn.equals(mimeOut))) {
                Log.i(TAG, "format didn't match on track No." + i +
                        formatSrc.toString() + "\n" + formatTest.toString());
            }
            extractorSrc.selectTrack(i);
            extractorTest.selectTrack(i);
        }
        // Pick a time and try to compare the frame.
        extractorSrc.seekTo(seekToUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        extractorTest.seekTo(seekToUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        int bufferSize = MAX_SAMPLE_SIZE;
        ByteBuffer byteBufSrc = ByteBuffer.allocate(bufferSize);
        ByteBuffer byteBufTest = ByteBuffer.allocate(bufferSize);
        extractorSrc.readSampleData(byteBufSrc, 0);
        extractorTest.readSampleData(byteBufTest, 0);
        if (!(byteBufSrc.equals(byteBufTest))) {
            Log.i(TAG, "byteBuffer didn't match");
        }
    }
}
