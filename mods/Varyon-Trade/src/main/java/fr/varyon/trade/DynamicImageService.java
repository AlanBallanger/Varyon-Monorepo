package fr.varyon.trade;

import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.setup.AssetFinalize;
import com.hypixel.hytale.protocol.packets.setup.AssetInitialize;
import com.hypixel.hytale.protocol.packets.setup.AssetPart;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class DynamicImageService
{
    private static final int MAX_PART_SIZE = 2621440;

    private static final String[] PATHS = {
            "UI/Custom/Pages/Dynamic/DynamicImage1.png", "UI/Custom/Pages/Dynamic/DynamicImage2.png",
            "UI/Custom/Pages/Dynamic/DynamicImage3.png", "UI/Custom/Pages/Dynamic/DynamicImage4.png",
            "UI/Custom/Pages/Dynamic/DynamicImage5.png", "UI/Custom/Pages/Dynamic/DynamicImage6.png",
            "UI/Custom/Pages/Dynamic/DynamicImage7.png", "UI/Custom/Pages/Dynamic/DynamicImage8.png",
            "UI/Custom/Pages/Dynamic/DynamicImage9.png", "UI/Custom/Pages/Dynamic/DynamicImage10.png"
    };

    private static final String[] HASHES = {
            "00456c6c696541555f4879554901000000000000000000000000000000000000",
            "00456c6c696541555f4879554902000000000000000000000000000000000000",
            "00456c6c696541555f4879554903000000000000000000000000000000000000",
            "00456c6c696541555f4879554904000000000000000000000000000000000000",
            "00456c6c696541555f4879554905000000000000000000000000000000000000",
            "00456c6c696541555f4879554906000000000000000000000000000000000000",
            "00456c6c696541555f4879554907000000000000000000000000000000000000",
            "00456c6c696541555f4879554908000000000000000000000000000000000000",
            "00456c6c696541555f4879554909000000000000000000000000000000000000",
            "00456c6c696541555f487955490A000000000000000000000000000000000000"
    };

    public static CompletableFuture<Void> sendLocalToInterfaceSlot(PlayerRef player, String localPath, int slotIndex)
    {
        if (player == null || !player.isValid())
            return CompletableFuture.failedFuture(new Exception("Currency icon not found"));

        if (slotIndex < 0 || slotIndex >= PATHS.length)
            return CompletableFuture.failedFuture(new Exception("Currency icon not found"));

        return CompletableFuture.runAsync(() ->
        {
            try
            {
                byte[] data = Files.readAllBytes(Paths.get(localPath));
                PacketHandler handler = player.getPacketHandler();

                CommonAsset placeholder = CommonAssetRegistry.getByName(PATHS[slotIndex]);
                if (placeholder != null)
                {
                    sendRawPackets(handler, placeholder, new byte[0]);
                }

                CommonAsset overrideAsset = new CommonAsset(PATHS[slotIndex], HASHES[slotIndex], data)
                {
                    @Override
                    protected CompletableFuture<byte[]> getBlob0()
                    {
                        return CompletableFuture.completedFuture(data);
                    }
                };

                sendRawPackets(handler, overrideAsset, data);
                handler.writeNoCache(new RequestCommonAssetsRebuild());
            }
            catch (Exception e) {}
        });
    }

    private static void sendRawPackets(PacketHandler handler, CommonAsset asset, byte[] data)
    {
        byte[][] parts = ArrayUtil.split(data, MAX_PART_SIZE);
        ToClientPacket[] packets = new ToClientPacket[2 + parts.length];

        packets[0] = new AssetInitialize(asset.toPacket(), data.length);
        for (int i = 0; i < parts.length; i++)
        {
            packets[1 + i] = new AssetPart(parts[i]);
        }
        packets[packets.length - 1] = new AssetFinalize();

        handler.write(packets);
    }
}

