package fr.varyon.extendedteleporters.files;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.util.io.BlockingDiskFile;
import fr.varyon.extendedteleporters.data.CustomDestination;
import fr.varyon.extendedteleporters.util.FileUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomDestinationFile extends BlockingDiskFile {
   private ConcurrentHashMap<String, CustomDestination> destinations = new ConcurrentHashMap<>();

   public CustomDestinationFile() {
      super(Path.of(FileUtils.CUSTOM_DESTINATIONS_PATH));
   }

   protected void read(BufferedReader reader) throws IOException {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      if (root != null) {
         this.destinations = new ConcurrentHashMap<>();
         JsonArray destinationsArray = root.getAsJsonArray("Destinations");
         if (destinationsArray != null) {
            destinationsArray.forEach(element -> {
               JsonObject obj = element.getAsJsonObject();
               String name = obj.get("Name").getAsString();
               String dimension = obj.get("Dimension").getAsString();
               double x = obj.get("X").getAsDouble();
               double y = obj.get("Y").getAsDouble();
               double z = obj.get("Z").getAsDouble();
               long createdTimestamp = obj.has("CreatedTimestamp") ? obj.get("CreatedTimestamp").getAsLong() : System.currentTimeMillis();
               String createdBy = obj.has("CreatedBy") && !obj.get("CreatedBy").isJsonNull() ? obj.get("CreatedBy").getAsString() : null;
               CustomDestination dest = new CustomDestination(name, dimension, x, y, z, createdTimestamp, createdBy);
               this.destinations.put(name.toLowerCase(), dest);
            });
         }
      }
   }

   protected void write(BufferedWriter writer) throws IOException {
      JsonObject root = new JsonObject();
      JsonArray destinationsArray = new JsonArray();
      this.destinations.values().forEach(dest -> {
         JsonObject obj = new JsonObject();
         obj.addProperty("Name", dest.name());
         obj.addProperty("Dimension", dest.dimension());
         obj.addProperty("X", dest.x());
         obj.addProperty("Y", dest.y());
         obj.addProperty("Z", dest.z());
         obj.addProperty("CreatedTimestamp", dest.createdTimestamp());
         if (dest.createdBy() != null) {
            obj.addProperty("CreatedBy", dest.createdBy());
         }

         destinationsArray.add(obj);
      });
      root.add("Destinations", destinationsArray);
      writer.write(root.toString());
   }

   protected void create(BufferedWriter writer) throws IOException {
      JsonObject root = new JsonObject();
      root.add("Destinations", new JsonArray());
      writer.write(root.toString());
   }

   public ConcurrentHashMap<String, CustomDestination> getDestinations() {
      return this.destinations;
   }
}
