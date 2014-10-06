package com.example.videoBuffer2;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.util.Map;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    Button btn;
    VideoView videoView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btn = (Button) findViewById(R.id.button);
        videoView = (VideoView) findViewById(R.id.videoView);
        MediaController mediaController = new
                MediaController(this);
        videoView.setMediaController(mediaController);


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startServer(1);


            }
        });
    }


    public void startServer(long fileLength) {

        final VideoStreamServer server = new VideoStreamServer(9090,fileLength);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoView.setVideoURI(Uri.parse("http://127.0.0.1:9090/1.mp4"));
        videoView.requestFocus();


        videoView.start();
    }


    public class VideoStreamServer extends NanoHTTPD
    {

        long fileLength;
        public VideoStreamServer(int port, long fileLength) {
            super(port);
            this.fileLength = fileLength;
        }

        private Response getPartialResponse(String mimeType, String rangeHeader) throws IOException {
            File file = new File("/mnt/sdcard","1-ffmpeg.mp4");
            String rangeValue = rangeHeader.trim().substring("bytes=".length());
            long fileLength = file.length();
            long start, end;
            if (rangeValue.startsWith("-")) {
                end = fileLength - 1;
                start = fileLength - 1
                        - Long.parseLong(rangeValue.substring("-".length()));
            } else {
                String[] range = rangeValue.split("-");
                start = Long.parseLong(range[0]);
                end = range.length > 1 ? Long.parseLong(range[1])
                        : fileLength - 1;
            }
            if (end > fileLength - 1) {
                end = fileLength - 1;
            }
            if (start <= end) {
                long contentLength = end - start + 1;
                //cleanupStreams();
                FileInputStream fileInputStream = new FileInputStream(file);
                //noinspection ResultOfMethodCallIgnored
                fileInputStream.skip(start);
                Response response = new Response(Response.Status.PARTIAL_CONTENT, mimeType, fileInputStream);
                response.addHeader("Content-Length", contentLength + "");
                response.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                Log.e("SERVER_PARTIAL", "bytes " + start + "-" + end + "/" + fileLength);
                response.addHeader("Content-Type", mimeType);
                return response;
            } else {
                return new Response(Response.Status.RANGE_NOT_SATISFIABLE, "video/mp4", rangeHeader);
            }
        }

        @Override
        public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {

            //range=bytes=619814-
            long range;
            int constantLength = 307200 ;
            long fileLength=0;
            boolean isLastPart=false;
            String rangeHeaderString="";

            if (headers.containsKey("range"))
            {
                String contentRange = headers.get("range");
                range = Integer.parseInt(contentRange.substring(contentRange.indexOf("=") + 1, contentRange.indexOf("-")));

            }
            else
            {
                range = 0;

            }



            byte[] buffer;


            long bufLength=0;


            try {

                RandomAccessFile ff =new RandomAccessFile(new File("/mnt/sdcard","1-ffmpeg.mp4"),"rw" );
                long remainingChunk = ff.length() - range; //remaining
                fileLength = ff.length();
                if (remainingChunk < constantLength){
                    bufLength= remainingChunk; //means last part
                    isLastPart=true;

                }

                else
                    bufLength = constantLength;
                if (range !=0)
                    ff.seek(range);
                buffer= new byte[(int)bufLength];


                ff.read(buffer);
                rangeHeaderString = String.format("bytes=%s-%s",range,range+bufLength);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                buffer = new byte[0];
            } catch (IOException e) {
                e.printStackTrace();
                buffer = new byte[0];
            }
            Response response;
//               if(!isLastPart)
                response = new Response(Response.Status.PARTIAL_CONTENT,"video/mp4",new ByteArrayInputStream(buffer));
//            else
//                   response = new Response(Response.Status.OK,"video/mp4",new ByteArrayInputStream(buffer));

            response.addHeader("Content-Length",(fileLength)+"");
            response.addHeader("Content-Range",String.format("bytes %s-%s/%s", range,(range+bufLength),fileLength));
            Log.e("SERVER", "Inside server sent " + String.format("bytes %s-%s/%s", range, (range + bufLength), fileLength));
            return response;
//            try {
//                Response res =getPartialResponse("video/mp4",rangeHeaderString);
//                return res;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return new Response(Response.Status.NOT_FOUND,"","");

        }
    }


}
