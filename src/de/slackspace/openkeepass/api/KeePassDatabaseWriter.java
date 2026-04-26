package de.slackspace.openkeepass.api;

import com.macmario.general.Version;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

import de.slackspace.openkeepass.crypto.CryptoInformation;
import de.slackspace.openkeepass.crypto.Decrypter;
import de.slackspace.openkeepass.crypto.ProtectedStringCrypto;
import de.slackspace.openkeepass.crypto.RandomGenerator;
import de.slackspace.openkeepass.crypto.Salsa20;
import de.slackspace.openkeepass.crypto.Sha256;
import de.slackspace.openkeepass.domain.KeePassFile;
import de.slackspace.openkeepass.domain.KeePassHeader;
import de.slackspace.openkeepass.domain.zipper.GroupZipper;
import de.slackspace.openkeepass.exception.KeePassDatabaseUnwriteableException;
import de.slackspace.openkeepass.parser.KeePassDatabaseXmlParser;
import de.slackspace.openkeepass.parser.SimpleXmlParser;
import de.slackspace.openkeepass.processor.EncryptionStrategy;
import de.slackspace.openkeepass.processor.ProtectedValueProcessor;
import de.slackspace.openkeepass.stream.HashedBlockOutputStream;

public class KeePassDatabaseWriter extends Version{

    private static final String UTF_8 = "UTF-8";

    public void writeKeePassFile(KeePassFile keePassFile, String pw, OutputStream stream) {
        final String func=getFunc("writeKeePassFile(KeePassFile keePassFile, String pw, OutputStream stream)");
        try {
            if (!validateKeePassFile(keePassFile)) {
                printf(func,1, "The provided keePassFile is not valid. A valid keePassFile must contain of meta and root group and the root group must at least contain one group." );
                throw new KeePassDatabaseUnwriteableException(
                        "The provided keePassFile is not valid. A valid keePassFile must contain of meta and root group and the root group must at least contain one group.");
            }
            
            printf(func,4," generate KeePassHeader" );
            KeePassHeader header = new KeePassHeader(new RandomGenerator());
            byte[] hashedPassword = hashPassword(pw);
            printf(func,4," generate password hashed");
            
            byte[] keePassFilePayload = marshallXml(keePassFile, header);
            printf(func,4,"payload generated");
            
            ByteArrayOutputStream streamToZip = compressStream(keePassFilePayload);
            printf(func,4,"payload compressStream generated");
            ByteArrayOutputStream streamToHashBlock = hashBlockStream(streamToZip);
            printf(func,4,"payload streamToHashBlock generated");
            ByteArrayOutputStream streamToEncrypt = combineHeaderAndContent(header, streamToHashBlock);
            printf(func,4,"payload streamToEncrypt generated");
            byte[] encryptedDatabase = encryptStream(header, hashedPassword, streamToEncrypt);
             printf(func,4,"payload encryptedDatabase generated - write now - pw:"+pw+":");
            
            // Write database to stream
            stream.write(encryptedDatabase);
            printf(func,4," Write database to stream - done" );
        } catch (IOException e) {
            printf(func,1,"ERROR: Could not write database file", e );
            throw new KeePassDatabaseUnwriteableException("Could not write database file", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private byte[] hashPassword(String password) throws UnsupportedEncodingException {
        byte[] passwordBytes = password.getBytes(UTF_8);
        return Sha256.hash(passwordBytes);
    }

    private byte[] encryptStream(KeePassHeader header, byte[] hashedPassword, ByteArrayOutputStream streamToEncrypt) throws IOException {
        CryptoInformation cryptoInformation = new CryptoInformation(KeePassHeader.VERSION_SIGNATURE_LENGTH, header.getMasterSeed(), header.getTransformSeed(),
                header.getEncryptionIV(), header.getTransformRounds(), header.getHeaderSize());

        return new Decrypter().encryptDatabase(hashedPassword, cryptoInformation, streamToEncrypt.toByteArray());
    }

    private ByteArrayOutputStream combineHeaderAndContent(KeePassHeader header, ByteArrayOutputStream content) throws IOException {
        ByteArrayOutputStream streamToEncrypt = new ByteArrayOutputStream();
        streamToEncrypt.write(header.getBytes());
        streamToEncrypt.write(header.getStreamStartBytes());
        streamToEncrypt.write(content.toByteArray());
        return streamToEncrypt;
    }

    private ByteArrayOutputStream hashBlockStream(ByteArrayOutputStream streamToUnzip) throws IOException {
        ByteArrayOutputStream streamToHashBlock = new ByteArrayOutputStream();
        HashedBlockOutputStream hashBlockOutputStream = new HashedBlockOutputStream(streamToHashBlock);
        hashBlockOutputStream.write(streamToUnzip.toByteArray());
        hashBlockOutputStream.close();
        return streamToHashBlock;
    }

    private byte[] marshallXml(KeePassFile keePassFile, KeePassHeader header) {
        final String func="marshallXml(KeePassFile keePassFile, KeePassHeader header)";
        printf(func,4, "create clonedKeePassFile");
        KeePassFile clonedKeePassFile = new GroupZipper(keePassFile).cloneKeePassFile();
        
        printf(func,4, "create protectedStringCrypto");
        ProtectedStringCrypto protectedStringCrypto = Salsa20.createInstance(header.getProtectedStreamKey());
        printf(func,4, "create ProtectedValueProcessor");
        new ProtectedValueProcessor().processProtectedValues(new EncryptionStrategy(protectedStringCrypto), clonedKeePassFile);
        printf(func,4, "create KeePassDatabaseXmlParser");
        KeePassDatabaseXmlParser kpdXml = new KeePassDatabaseXmlParser(new SimpleXmlParser());
        printf(func,4, "toXML KeePassDatabaseXmlParser");
        byte[] b = kpdXml.toXml(keePassFile).toByteArray();
        printf(func,4, "return byte[] b:"+b.length);
        return b;
    }

    private ByteArrayOutputStream compressStream(byte[] keePassFilePayload) throws IOException {
        ByteArrayOutputStream streamToZip = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(streamToZip);
        gzipOutputStream.write(keePassFilePayload);
        gzipOutputStream.close();
        return streamToZip;
    }

    private static boolean validateKeePassFile(KeePassFile keePassFile) {
        if (keePassFile == null || keePassFile.getMeta() == null) {
            return false;
        }

        if (keePassFile.getRoot() == null || keePassFile.getRoot().getGroups().isEmpty()) {
            return false;
        }

        return true;
    }
}
