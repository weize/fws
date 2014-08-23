infile <- "../exp/expansion-res/gmi/gmi.sum-9.annotator.fdbk/expansion.ffs.tceval.at50"
data <- read.csv(infile, header=F, sep="\t")
metrics <- c("MAP", "MRR", "P10", "P20", "NDCG10", "NDCG20", "MAP10", "MAP20")
names(data)[names(data)=="V1"] <- "qid"
names(data)[names(data)=="V2"] <- "time" 
names(data)[names(data)=="V3"] <- metrics[1]
names(data)[names(data)=="V4"] <- metrics[2]
names(data)[names(data)=="V5"] <- metrics[3]
names(data)[names(data)=="V6"] <- metrics[4]
names(data)[names(data)=="V7"] <- metrics[5]
names(data)[names(data)=="V8"] <- metrics[6]
names(data)[names(data)=="V9"] <- metrics[7]
names(data)[names(data)=="V10"] <- metrics[8]

qf <- data[data$qid!="all",]

infile <- "../exp/srank/sdm/sdm.srank.qavgeval"
data <- read.csv(infile, header=T, sep="\t")
names(data)[names(data)=="X.qid"] <- "qid"
names(data)[names(data)=="map"] <- metrics[1] 
names(data)[names(data)=="recip_rank"] <- metrics[2]
names(data)[names(data)=="P_10"] <- metrics[3]
names(data)[names(data)=="P_20"] <- metrics[4]
names(data)[names(data)=="ndcg_cut_10"] <- metrics[5]
names(data)[names(data)=="ndcg_cut_20"] <- metrics[6]
names(data)[names(data)=="map_cut_10"] <- metrics[7]
names(data)[names(data)=="map_cut_20"] <- metrics[8]

sdm <- data[data$qid!="all",]

infile <- "../exp/srank/prm/prm.srank.qavgeval"
data <- read.csv(infile, header=T, sep="\t")
names(data)[names(data)=="X.qid"] <- "qid"
names(data)[names(data)=="map"] <- metrics[1] 
names(data)[names(data)=="recip_rank"] <- metrics[2]
names(data)[names(data)=="P_10"] <- metrics[3]
names(data)[names(data)=="P_20"] <- metrics[4]
names(data)[names(data)=="ndcg_cut_10"] <- metrics[5]
names(data)[names(data)=="ndcg_cut_20"] <- metrics[6]
names(data)[names(data)=="map_cut_10"] <- metrics[7]
names(data)[names(data)=="map_cut_20"] <- metrics[8]

prf <- data[data$qid!="all",]

t.test(qf$MAP, prf$MAP, paired=T)


infile <- "../exp/expansion-res/annotator/annotator.annotator.fdbk/expansion.ffs.tceval.at10"
data <- read.csv(infile, header=F, sep="\t")
names(data)[names(data)=="V1"] <- "qid"
names(data)[names(data)=="V2"] <- "time" 
names(data)[names(data)=="V3"] <- metrics[1]
names(data)[names(data)=="V4"] <- metrics[2]
names(data)[names(data)=="V5"] <- metrics[3]
names(data)[names(data)=="V6"] <- metrics[4]
names(data)[names(data)=="V7"] <- metrics[5]
names(data)[names(data)=="V8"] <- metrics[6]
names(data)[names(data)=="V9"] <- metrics[7]
names(data)[names(data)=="V10"] <- metrics[8]

annotator <- data[data$qid!="all",]

infile <- "../java/annotator.50"
data <- read.csv(infile, header=F, sep="\t")
names(data)[names(data)=="V1"] <- "qid"
names(data)[names(data)=="V2"] <- "time" 
names(data)[names(data)=="V3"] <- metrics[1]
names(data)[names(data)=="V4"] <- metrics[2]
names(data)[names(data)=="V5"] <- metrics[3]
names(data)[names(data)=="V6"] <- metrics[4]
names(data)[names(data)=="V7"] <- metrics[5]
names(data)[names(data)=="V8"] <- metrics[6]
names(data)[names(data)=="V9"] <- metrics[7]
names(data)[names(data)=="V10"] <- metrics[8]

annotator <- data[data$qid!="all",]



infile <- "../exp/srank/pm2/pm2.srank.qavgeval"
data <- read.csv(infile, header=T, sep="\t")
names(data)[names(data)=="X.qid"] <- "qid"
names(data)[names(data)=="map"] <- metrics[1] 
names(data)[names(data)=="recip_rank"] <- metrics[2]
names(data)[names(data)=="P_10"] <- metrics[3]
names(data)[names(data)=="P_20"] <- metrics[4]
names(data)[names(data)=="ndcg_cut_10"] <- metrics[5]
names(data)[names(data)=="ndcg_cut_20"] <- metrics[6]
names(data)[names(data)=="map_cut_10"] <- metrics[7]
names(data)[names(data)=="map_cut_20"] <- metrics[8]

pm2 <- data[data$qid!="all",]
