loadTcEval <- function(fdbkSrc, fdbkParam, facetSrc, facetParam, expModel) {
	#gmi.sum-11.annotator.fdbk
	name1 <- if(fdbkParam=="") fdbkSrc else sprintf("%s.%s", fdbkSrc, fdbkParam)
	name2 <- if(facetParam=="") facetSrc else sprintf("%s.%s", facetSrc, facetParam)
	infile <- sprintf("../exp/expansion-res/%s/%s.%s.fdbk/expansion.%s.qavgtceval", fdbkSrc, name1, name2, expModel)
	data <- read.csv(infile, header=F, sep="\t")
	metrics <- c("MAP", "MRR", "P@10", "P@20", "NDCG@10", "NDCG@20", "MAP@10", "MAP@20")
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
	data
}

loadTermSelectedEval <- function(fdbkSrc, fdbkParam, facetSrc, facetParam) {
	#gmi.sum-11.annotator.fdbk
	name1 <- if(fdbkParam=="") fdbkSrc else sprintf("%s.%s", fdbkSrc, fdbkParam)
	name2 <- if(facetParam=="") facetSrc else sprintf("%s.%s", facetSrc, facetParam)
	infile <- sprintf("../exp/data/cmp-facet/%s.%s.selectTermNum", name1, name2)
	data <- read.csv(infile, header=F, sep="\t")
	names(data)[names(data)=="V1"] <- "qid"
	names(data)[names(data)=="V2"] <- "time" 
	names(data)[names(data)=="V3"] <- "term" 
	data[data$time<=50,]
}

loadCumTermOverFacet <- function(facetSrc, facetParam) {
	#gmi.sum-11.annotator.fdbk
	name2 <- if(facetParam=="") facetSrc else sprintf("%s.%s", facetSrc, facetParam)
	infile <- sprintf("../exp/data/cmp-facet/%s.accuTermOverFacet", name2)
	data <- read.csv(infile, header=F, sep="\t")
	names(data)[names(data)=="V1"] <- "qid"
	names(data)[names(data)=="V2"] <- "time" # number of facet actually
	names(data)[names(data)=="V3"] <- "cumTerm" 
	data[data$time<=15,]
}

getFacetParam <- function(facetSrc) {
	facetParam <- ""

	if (facetSrc == "annotator") {
		facetParam <- ""
	} else if (facetSrc == "gmi") {
		facetParam <- "sum-9"
	} else if (facetSrc == "gmj") {
		facetParam <- "sum"
	} else {
		facetParam <- "9"
	}
	facetParam
}

getFeedbackParam <- function(feedbackSrc) {
	param <- ""

	if (feedback== "annotator") {
		param <- ""
	} else {
		param <- "sts-0_01"
	}
	param	
}
