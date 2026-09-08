package co.edu.uniminuto.hyeda.documents.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

/**
 * Generacion de codigos QR: para cobrar y para verificar la autenticidad de un
 * documento impreso.
 */
@Service
public class QrCodeService {

    public byte[] pngBytes(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el codigo QR: " + e.getMessage());
        }
    }

    /** Data URI listo para incrustar en el HTML que se convierte a PDF. */
    public String dataUri(String content, int size) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes(content, size));
    }
}
