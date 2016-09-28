/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package security

import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api.util.PasswordInfo
import java.security.SecureRandom
import org.apache.shiro.crypto.SecureRandomNumberGenerator
import org.apache.shiro.crypto.hash.Sha256Hash
import org.apache.shiro.authc.credential.HashedCredentialsMatcher
import org.apache.shiro.authc.SaltedAuthenticationInfo
import org.apache.shiro.authc.SimpleAuthenticationInfo
import org.apache.shiro.codec.Base64

class ShiroPasswordHasher extends PasswordHasher {
  val ID = "shiro-sha256"

  private val shiroRng = new SecureRandomNumberGenerator

  def id = ID

  def hash(plainPassword: String) = {
    val salt = shiroRng.nextBytes()

    val passwordHashBase64 = new Sha256Hash(plainPassword, salt, 1024).toBase64
    val saltBase64 = salt.toBase64

    PasswordInfo(id, passwordHashBase64, Some(saltBase64))
  }

  def matches(passwordInfo: PasswordInfo, suppliedPassword: String) = {
    val saltBytes = passwordInfo.salt match {
      case Some(salt) => Base64.decode(passwordInfo.salt.get)
      case None => throw new DatabaseFormatException("Salt field cannot be empty for Shiro hashed passwords")
    }

    val passwordHashBase64 = new Sha256Hash(suppliedPassword, saltBytes, 1024).toBase64

    passwordHashBase64 == passwordInfo.password
  }

  def isDeprecated(passwordInfo: PasswordInfo): Option[Boolean] = Some(false)
}
