package com.iscas.biz.util;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.util.Properties;
import java.util.Vector;

@Slf4j
public class SftpUtil {


    private ChannelSftp sftp;

    private JSch jsch;

    private Session sshSession;

    public SftpUtil(String host, int port, String username, String password) {
        sessionConnect(host, port, username, password);
    }

    /**
     * 连接sftp服务器
     *
     * @param host     主机
     * @param port     端口
     * @param username 用户名
     * @param password 密码
     */
    public void sessionConnect(String host, int port, String username, String password) {
        try {
            this.jsch = new JSch();
            this.jsch.addIdentity("src/main/resources/136k");//设置为密钥
            this.sshSession = jsch.getSession(username, host, port);
            //this.sshSession.setPassword(password);//密码登录
            Properties sshConfig = new Properties();
            //sshConfig.put("StrictHostKeyChecking", "no");
            //this.sshSession.setConfig(sshConfig);
            this.sshSession.setConfig("userauth.gssapi-with-mic", "no");
            this.sshSession.setConfig("StrictHostKeyChecking", "no"); //指定密钥验证方式。
            this.sshSession.setConfig("PreferredAuthentications", "publickey"); //指定密钥验证方式。
            this.sshSession.connect();
        } catch (Exception e) {
            log.error("SftpUtil Session host=[" + host + "];port=[" + port + "];user=[" + username
                    + "];passwd=[" + password + "] error : ", e);
        }
    }


    /**
     * 连接sftp
     */
    public SftpUtil sftpConnect() {
        if (sftp == null) {
            try {
                Channel channelSftp = this.sshSession.openChannel("sftp");
                this.sftp = (ChannelSftp) channelSftp;
                sftp.connect();
            } catch (Exception e) {
                log.error("SftpUtil ChannelSftp error : ", e.getMessage());
            }
        }
        return this;
    }

    /**
     * 断开sftp服务器
     */
    public void disConnect() {

        try {
            if (sftp != null) {
                sftp.disconnect();
                sftp.exit();
            }
            sshSession.disconnect();
            log.debug("断开sftp服务器 ... ");
        } catch (Exception e) {
            log.error("断开sftp服务器 error : ", e);
        }
    }

    /**
     * 创建目录
     *
     * @param directory 要创建的目录的父目录
     * @param dirName   要创建的目录的名称
     */
    public boolean mkDir(String directory, String dirName) {
        boolean success = false;
        try {
            this.sftp.cd(directory);
            this.sftp.mkdir(dirName);
            success = true;
        } catch (Exception e) {
            log.error("创建文件夹 directory=[" + directory + "];dirName=[" + dirName + "] error : ", e);
        }
        return success;
    }


    /**
     * 上传文件
     *
     * @param src      要上传的文件流
     * @param dir      上传的目录
     * @param fileName 文件名称
     */
    public boolean uploadStream(InputStream src, String dir, String fileName) throws Exception {
        boolean ok = false;
        try {
            this.sftp.cd(dir);
            this.sftp.put(src, fileName);
            ok = true;
        } catch (Exception e) {
            throw new RuntimeException("上传文件异常," + "upload directory=[" + dir + "];uploadFile=[" + fileName + "] error:" + e.getMessage());
        } finally {
            try {
                if (src != null) {
                    src.close();
                }
            } catch (IOException e) {
                log.error("SftpUtil upload directory=[" + dir + "];uploadFile=[" + fileName
                        + "] close FileInputStream error : ", e);
            }
        }
        return ok;
    }


    /**
     * 上传文件
     *
     * @param directory  上传的目录
     * @param uploadFile 要上传的文件
     */
    public boolean upload(String directory, String uploadFile) throws Exception {
        boolean success = false;
        FileInputStream fis = null;
        try {
            this.sftp.cd(directory);
            File file = new File(uploadFile);
            fis = new FileInputStream(file);
            this.sftp.put(fis, file.getName());
            success = true;
        } catch (Exception e) {
            log.error("上传文件 directory=[" + directory + "];uploadFile=[" + uploadFile + "] error : ", e);
            throw e;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                log.error("上传文件 directory=[" + directory + "];uploadFile=[" + uploadFile
                        + "] close FileInputStream error : ", e);
            }
        }

        return success;
    }

    /**
     * 下载文件
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件
     * @param saveFile     存在本地的路径
     */
    public boolean download(String directory, String downloadFile, String saveFile) {
        boolean success = false;
        FileOutputStream fos = null;
        try {
            this.sftp.cd(directory);
            File file = new File(saveFile);
            fos = new FileOutputStream(file);
            this.sftp.get(downloadFile, fos);
            success = true;
        } catch (Exception e) {
            log.error("下载文件 directory=[" + directory + "];downloadFile=[" + downloadFile + "] error : ", e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                log.error("下载文件 directory=[" + directory + "];downloadFile=[" + downloadFile
                        + "] close FileOutputStream error : ", e);
            }
        }
        return success;
    }

    /**
     * 列出目录下的文件
     *
     * @param directory 要列出的目录
     */
    public Vector listFiles(String directory) throws SftpException {
        return this.sftp.ls(directory);
    }

    /**
     * 删除文件
     *
     * @param directory  要删除文件所在目录
     * @param deleteFile 要删除的文件
     */
    public boolean delete(String directory, String deleteFile) {
        boolean success = false;
        try {
            this.sftp.cd(directory);
            this.sftp.rm(deleteFile);
            success = true;
        } catch (Exception e) {
            log.error("删除文件 directory=[" + directory + "];deleteFile=[" + deleteFile + "] error : ", e);
        }
        return success;
    }

    /**
     * 删除目录
     *
     * @param directory 要删除文件所在目录
     */
    public boolean deleteDir(String directory) {
        boolean success = false;
        try {
            this.sftp.rmdir(directory);
            success = true;
        } catch (Exception e) {
            log.error("删除目录 directory=[" + directory + "] error : ", e);
        }
        return success;
    }

    /**
     * 列出目录下所有的文件
     *
     * @param directory 要列出的目录
     */
    public Vector listAllFiles(String directory) throws SftpException {
        return this.sftp.ls(directory);
    }

    public static void main(String[] args) {
        SftpUtil sftp = null;
        try {
            //创建sftp
            SftpUtil sftpUtil = new SftpUtil("192.168.66.136", 22, "mysftp", "123456");
            sftp = sftpUtil.sftpConnect();
            //创建文件夹
            //boolean mkdir = sftp.mkDir("/home/iscas/test", "a");
            //System.out.println("创建文件夹 :" + mkdir);
            ////上传文件
            //boolean upload = sftp.upload("/home/iscas/test/a", "C:\\Users\\x\\Desktop\\quick-frame-samples.sql");
            //System.out.println("上传文件 :" + upload);
            ////下载文件
            //boolean download = sftp.download("/home/iscas/test/a", "quick-frame-samples.sql", "C:\\Users\\x\\Desktop\\sftp\\test.sql");
            //System.out.println("下载文件 :" + download);
            ////列出目录下的文件
            Vector files = sftp.listFiles(".");
            System.out.println("files: " + files);
            //System.out.println("文件个数：" + files.size());
            ////删除文件
            //boolean delete = sftp.delete("/home/iscas/test/a", "quick-frame-samples.sql");
            //System.out.println("删除文件 :" + delete);
            ////删除目录
            //boolean deleteDir = sftp.deleteDir("/home/iscas/test/a");
            //System.out.println("删除目录 :" + deleteDir);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sftp != null) {
                sftp.disConnect();
            }
        }

    }


}

