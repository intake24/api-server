package controllers.system.asynchronous

import java.io.{File, FileInputStream, IOException}
import java.net.SocketException
import javax.inject.Singleton

import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.util.TrustManagerUtils

case class FTPSConfig(host: String, port: Int, userName: String, password: String, pathPrefix: String)

@Singleton
class DataExportFtpsUploader {
  def upload(file: File, remoteName: String, config: FTPSConfig): Either[Throwable, Unit] = {
    try {
      val ftpsClient = new FTPSClient()

      ftpsClient.setTrustManager(TrustManagerUtils.getValidateServerCertificateTrustManager)

      ftpsClient.connect(config.host)
      ftpsClient.enterLocalPassiveMode()
      ftpsClient.login(config.userName, config.password)
      ftpsClient.storeFile(config.pathPrefix + "/" + remoteName, new FileInputStream(file))
      ftpsClient.logout()
      ftpsClient.disconnect()

      Right(())
    } catch {
      case se: SocketException => Left(se)
      case ioe: IOException => Left(ioe)
    }
  }
}
