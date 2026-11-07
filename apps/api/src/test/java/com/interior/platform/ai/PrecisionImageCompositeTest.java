package com.interior.platform.ai;

import com.interior.platform.ai.service.PrecisionImageComposite;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class PrecisionImageCompositeTest {
    private byte[] png(int width, int height, int color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setRGB(x,y,color);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image,"png",bytes);
        return bytes.toByteArray();
    }
    @Test void keepsEveryUnselectedSourcePixelEvenWhenProviderChangesTheWholeRoom() throws Exception {
        byte[] source = png(3,2,0xff386241), generated = png(3,2,0xff101010);
        BufferedImage mask = ImageIO.read(new ByteArrayInputStream(png(3,2,0xff000000)));
        mask.setRGB(1,0,0xffffffff);
        ByteArrayOutputStream maskBytes = new ByteArrayOutputStream();
        ImageIO.write(mask,"png",maskBytes);
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(PrecisionImageComposite.preserveUnselected(source, generated, maskBytes.toByteArray())));
        for (int y=0; y<2; y++) for(int x=0; x<3; x++) assertEquals(x==1 && y==0 ? 0xff101010 : 0xff386241,result.getRGB(x,y));
    }
    @Test void rejectsMisalignedProviderOutput() throws Exception {
        byte[] source=png(3,2,0xff000000), generated=png(2,2,0xff000000), mask=png(3,2,0xffffffff);
        assertThrows(IOException.class, () -> PrecisionImageComposite.preserveUnselected(source,generated,mask));
    }
}
