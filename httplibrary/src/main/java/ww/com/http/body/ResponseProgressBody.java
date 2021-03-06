package ww.com.http.body;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import ww.com.http.bean.Progress;
import ww.com.http.core.RequestConstants;
import ww.com.http.interfaces.DownloadProgressListener;

public class ResponseProgressBody extends ResponseBody {

    private final ResponseBody mResponseBody;
    private BufferedSource bufferedSource;
    private DownloadProgressHandler downloadProgressHandler;

    public ResponseProgressBody(ResponseBody responseBody, DownloadProgressListener downloadProgressListener) {
        this.mResponseBody = responseBody;
        if (downloadProgressListener != null) {
            this.downloadProgressHandler = new DownloadProgressHandler(downloadProgressListener);
        }
    }

    @Override
    public MediaType contentType() {
        return mResponseBody.contentType();
    }

    @Override
    public long contentLength() {
        return mResponseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        System.out.println("source():1");
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(mResponseBody.source()));
        }
        System.out.println("source():2");
        return bufferedSource;
    }

    private Source source(Source source) {

        return new ForwardingSource(source) {

            long totalBytesRead;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                totalBytesRead += ((bytesRead != -1) ? bytesRead : 0);
                if (downloadProgressHandler != null) {
                    downloadProgressHandler.obtainMessage(RequestConstants.UPDATE,
                            new Progress(totalBytesRead, mResponseBody.contentLength(), bytesRead == -1))
                            .sendToTarget();
                }
                return bytesRead;
            }
        };
    }
}