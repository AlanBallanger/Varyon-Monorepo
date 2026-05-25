package fr.varyon.playermarker;

import com.hypixel.hytale.server.core.asset.common.CommonAsset;

import java.util.concurrent.CompletableFuture;

final class PlayerMarkerRuntimePngAsset extends CommonAsset {

    private final byte[] pngBytes;

    PlayerMarkerRuntimePngAsset(String assetPath, byte[] pngBytes) {
        super(assetPath, pngBytes);
        this.pngBytes = pngBytes != null ? pngBytes.clone() : new byte[0];
    }

    @Override
    protected CompletableFuture<byte[]> getBlob0() {
        return CompletableFuture.completedFuture(pngBytes.clone());
    }
}