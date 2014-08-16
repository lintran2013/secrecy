/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.doplgangr.secrecy.FileSystem;

/**
 * This is an alias of a FILE in the secrecy system.
 *
 */


import android.graphics.Bitmap;
import android.util.Log;

import com.doplgangr.secrecy.Config;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.CipherInputStream;

public class File {
    public static final String NAME = "FILENAME";
    public static final String TYPE = "FILETYPE";
    public static final String FILETIMESTAMP = "FILETIMESTAMP";
    public static final String FILESIZE = "FILESIZE";
    public Boolean decrypting = false;
    private String name;
    private String size;
    private Date Timestamp;
    private String FileType;
    private java.io.File file;
    private java.io.File thumbnailRealFile;
    private Bitmap thumb = null;
    private Boolean invalidFile = false;
    private String key;

    public File(java.io.File file, String secret) {
        if (file.exists()) {
            name = FilenameUtils.getBaseName(file.getName());
            FileType = FilenameUtils.getExtension(file.getName());
            this.file = file;
            this.key = secret;
            String path = file.getParent();
            this.size = humanReadableByteCount(file.length());
            thumbnailRealFile = new java.io.File(path + "/_thumb" + file.getName());
            Timestamp = new Date(file.lastModified());
        } else {
            invalidFile = true;
        }
    }

    private static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = ("KMGTPE").charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public Bitmap getThumb() {
        File thumbnailFile = new File(thumbnailRealFile, key);
        if ((!thumbnailFile.invalidFile) && (thumb == null)) {
            java.io.File tempThumb = thumbnailFile.readFile(new CryptStateListener() {
                @Override
                public void updateProgress(int progress) {
                }

                @Override
                public void setMax(int max) {
                }

                @Override
                public void onFailed(int statCode) {

                }

                @Override
                public void Finished() {

                }
            });
            this.thumb = storage.getThumbnail(tempThumb);
            if (tempThumb != null)
                tempThumb.delete();
        }
        return thumb;
    }

    public boolean hasThumbnail() {
        File thumbnailFile = new File(thumbnailRealFile, key);
        return (!thumbnailFile.invalidFile);
    }

    public String getType() {
        return FileType;
    }

    public java.io.File getFile() {
        return file;
    }

    public String getTimestamp() {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return df.format(Timestamp);
    }

    public java.io.File readFile(CryptStateListener listener) {
        decrypting = true;
        InputStream is = null;
        OutputStream out = null;
        java.io.File outputFile = null;
        try {
            outputFile = java.io.File.createTempFile("tmp" + name, "." + FileType, storage.getTempFolder());
            AES_Encryptor enc = new AES_Encryptor(key);
            is = new CipherInputStream(new FileInputStream(file), enc.decryptstream());
            listener.setMax((int) file.length());
            byte buffer[] = new byte[Config.bufferSize];
            int count;
            out = new BufferedOutputStream(new FileOutputStream(outputFile));
            while ((count = is.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                listener.updateProgress((int) outputFile.length());
            }
            return outputFile;
        } catch (FileNotFoundException e) {
            listener.onFailed(2);
        } catch (IOException e) {
            Log.d("IO", "Exception");
            if (e.getMessage().contains("pad block corrupted"))
                listener.onFailed(1);
            else
                e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            listener.Finished();
            decrypting = false;
            try {
                if (is != null) {
                    is.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // An error occured. Too Bad
        if (outputFile != null)
            outputFile.delete();
        return null;
    }

    public void delete() {
        file.delete();
        File thumbnailFile = new File(thumbnailRealFile, key);
        if (!thumbnailFile.invalidFile)
            thumbnailFile.delete();
    }
}
