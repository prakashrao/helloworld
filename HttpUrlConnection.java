
public class HttpUrlConnection {

    private static final String TAG = VenndHttpUrlConnection.class.getSimpleName();

    public static String httpConnect(String Url) {
        if (Dlog.WRITE_LOG_TO_FILE)
            Dlog.log(TAG, "connect Url:"+Url);
        String ret = "Http Error";
        try {
            URL url = new URL(Url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                ret = stringBuilder.toString();
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            Log.d(TAG, "connect ret:"+ret);
        return ret;
    }

    public static String httpConnect(String Url, final FileDetails fileDetails, HttpListener listener) {
        String ret = "Http Error";
        if (Dlog.WRITE_LOG_TO_FILE)
            Dlog.log(TAG, "connect2 Url:"+Url);
        File sourceFile = new File(fileDetails.getFilePath());

        listener.onPreExecute(fileDetails,fileDetails.isOnline());
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection connection = null;
        String fileName = sourceFile.getName();
        int mTotalSize = (int) sourceFile.length();

        try {
            connection = (HttpURLConnection) new URL(Url).openConnection();
            connection.setRequestMethod("POST");
            String boundary = "---------------------------boundary";
            String tail = "\r\n--" + boundary + "--\r\n";
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);
            String metadataPart = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"metadata\"\r\n\r\n"
                    + "" + "\r\n";

            String fileHeader1 = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"fileToUpload\"; filename=\""
                    + fileName + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n"
                    + "Content-Transfer-Encoding: binary\r\n";

            long fileLength = sourceFile.length() + tail.length();
            String fileHeader2 = "Content-length: " + fileLength + "\r\n";
            String fileHeader = fileHeader1 + fileHeader2 + "\r\n";
            String stringData = metadataPart + fileHeader;
            long requestLength = stringData.length() + fileLength;
            connection.setRequestProperty("Content-length", "" + requestLength);
            connection.setFixedLengthStreamingMode((int) requestLength);
            connection.connect();
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(stringData);
            out.flush();

            int progress = 0;
            int bytesRead = 0;
            byte buf[] = new byte[1024];
            BufferedInputStream bufInput = new BufferedInputStream(new FileInputStream(sourceFile));
            while ((bytesRead = bufInput.read(buf)) != -1) {
                // write output
                out.write(buf, 0, bytesRead);
                out.flush();
                progress += bytesRead; // Here progress is total uploaded bytes
                listener.onProgressUpgrade(fileDetails.getFileName(), fileDetails.isOnline(), (int) ((progress / (float) mTotalSize) * 100));
               // publishProgress((int) ((progress / (float) mTotalSize) * 100));
            }

            // Write closing boundary and close stream
            out.writeBytes(tail);
            out.flush();
            out.close();
            // Get server response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            ret = builder.toString();
                Log.d(TAG , "connect2 mUploadResponse:"+ ret);
        } catch (Exception e) {
                e.printStackTrace();
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return ret;
    }

}
