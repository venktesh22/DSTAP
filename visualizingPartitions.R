library(plyr)
library(leaflet)
library (dplyr)
library (RColorBrewer)

#netName <- "Austin_sdb"
#partFileName <- "SDDA_partition"

netName <- "Texas"
partFileName <- "Manual_partitions_TX"

CoordinateFileName <- paste("Networks/",netName,"/",netName,"_node.txt",sep="")
PartitionFileName <- paste("Networks/",netName,"/",partFileName,".txt",sep="")
nodeXY <- read.csv(CoordinateFileName, header=TRUE, 
                   sep = "\t")
nodeToCluster <- read.csv(PartitionFileName, header=TRUE, sep="\t")
nodeToCluster <- plyr::rename(nodeToCluster, c("NodeID"="Node"))

merged_data <- nodeXY
merged_data <- merge(nodeXY, nodeToCluster, by="Node")

#pal <- colorFactor(c("navy", "red"), 0:1)
factpal <- colorFactor(palette = c("blue", "red"), merged_data$ClusterNumber)
factpal <- colorFactor(palette = brewer.pal(n = 4, name = "RdBu"), merged_data$ClusterNumber)


mapNetwork <- leaflet(merged_data) %>%
  addTiles() %>%
  #addProviderTiles("CartoDB.Positron") %>%
      setView(lat = 30.238703,
              lng = -97.756708,
              zoom = 6) %>%
      addCircles(
        lng = merged_data[,2],
        lat = merged_data[,3],
        opacity = 0.1,
        #radius = 0.1,
        color = ~factpal(ClusterNumber),
        # popup = paste("IP: ", sensors$db_ip, "<br>", "Street: ", sensors$id),
        popup = paste("Node: ", merged_data$Node, " and subnetID: ", merged_data$ClusterNumber)
      )#%>%
     #addLegend(colors = c("navy","red"), labels = c("IP Yes","IP No"), opacity = 1)

mapNetwork

# ipaddress_Cameras <- read.csv("C:/Users/vp6258/Box Sync/UT Acads/Summer 2017/Video recognition project/cameras.csv")
# 
# ipaddress_Cameras <- plyr::rename(ipaddress_Cameras, c("ATD_CAMERA_ID"="CAMERA_ID"))
# 
# merged_data <- merge(ipaddress_Cameras,latlongdata_Cameras,by="CAMERA_ID")
# 
# write.csv(merged_data,"Traffic_cameras_metadata.csv")
# 
# videoMapAustin <- leaflet() %>%
#     addProviderTiles("CartoDB.Positron") %>%
#     setView(lat = 30.238703,
#             lng = -97.756708,
#             zoom = 13) %>%
#     addCircleMarkers(
#       lng = merged_data$LONGITUDE,
#       lat = merged_data$LATITUDE,
#       color= "red",
#       # popup = paste("IP: ", sensors$db_ip, "<br>", "Street: ", sensors$id),
#       popup = paste("Street: ", merged_data$PRIMARY_ST," and Camera ID: ", merged_data$CAMERA_ID, " and IP address: ",merged_data$IP)
#     ) %>%
#   addCircleMarkers(
#     lng=latlongdata_Cameras$LONGITUDE,
#     lat= latlongdata_Cameras$LATITUDE,
#     popup = paste("Street: ", latlongdata_Cameras$PRIMARY_ST," and Camera ID: ", latlongdata_Cameras$CAMERA_ID)
#   ) %>%
#   addLegend(colors = c("red","blue"), labels = c("IP Yes","IP No"), opacity = 1)
# videoMapAustin
# 
# large_data <- read.csv("C:/Users/vp6258/Box Sync/UT Acads/Research/GRA/Consistent modelling TxDOT/TM4 work/SAM_2040.csv")
