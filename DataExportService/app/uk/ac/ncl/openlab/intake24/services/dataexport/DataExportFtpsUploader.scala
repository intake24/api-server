package uk.ac.ncl.openlab.intake24.services.dataexport

import java.io.{File, FileInputStream, IOException}
import java.net.SocketException

import javax.inject.Singleton
import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.util.TrustManagerUtils

case class FTPSConfig(host: String, port: Int, userName: String, password: String, pathPrefix: String)

@Singleton
class DataExportFtpsUploader {

  private def throwOnError(ftpsClient: FTPSClient)(block: => Boolean): Unit = if (!block) {
    throw new IOException(ftpsClient.getReplyString)
  }

  def upload(file: File, remoteName: String, config: FTPSConfig): Either[Throwable, Unit] = {
    val ftpsClient = new FTPSClient()
    try {
      ftpsClient.setTrustManager(TrustManagerUtils.getValidateServerCertificateTrustManager)
      ftpsClient.connect(config.host)
      ftpsClient.enterLocalPassiveMode()

      throwOnError(ftpsClient) {
        ftpsClient.login(config.userName, config.password)
      }

      throwOnError(ftpsClient) {
        ftpsClient.storeFile(config.pathPrefix + "/" + remoteName, new FileInputStream(file))
      }

      ftpsClient.logout()

      Right(())
    } catch {
      case se: SocketException => Left(se)
      case ioe: IOException => Left(ioe)
    } finally {
      ftpsClient.disconnect()
    }
  }
}
