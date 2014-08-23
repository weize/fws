fitFeedbackTime<- function(aid) {
	infile = "../exp/annotation/from-db/feedback-time"; 
	#outfile = sprintf("figure/fit-feedback-time.%s.eps", aid)

	data <- read.csv(infile, header=F, sep="\t")
	names(data)[names(data)=="V1"] <- "aid"
	names(data)[names(data)=="V2"] <- "qid"
	names(data)[names(data)=="V3"] <- "sid"
	names(data)[names(data)=="V4"] <- "facet"
	names(data)[names(data)=="V5"] <- "term"
	names(data)[names(data)=="V6"] <- "time"
	data$time <-data$time / 1000
	if (aid != 'all') {
		data = data[data$aid==aid, ]
	}
	fit <- lm(time ~ 0 + facet + term, data)
	print(fit$coefficients)
	summary(fit)
}

args <- commandArgs(T)
aid <- args[1]

fitFeedbackTime(aid)
