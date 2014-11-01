source('functions.R')

args <- commandArgs(T)
facetSrc <- args[1]
metricName <- args[2]


#metricName <- "MAP"
feedbackSrc <- "annotator"
feedbackParam <- ""
facetParam <- getFacetParam(facetSrc)

outfile <- sprintf("figure/presentation.cmp-expansion-%s-%s-%s.eps", facetSrc,feedbackSrc, metricName)
#title <- sprintf("Cmp expasion. facet=%s, fdbk=%s,\n  m=%s", facetSrc, feedbackSrc, metricName)
title <- ""

ffs<- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, "ffs")
fts<- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, "fts")
ftor<- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, "ftor")
ftandor<- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, "ftandor")
ftand<- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, "ftand")

all <- list(ffs, fts, ftor, ftandor, ftand)
dataNum <- length(all) 
names <- c("SF", "ST", "OR", "A+R", "AND")
colors <- rainbow(dataNum)
plotchar <- seq(18,18+dataNum,1)
linetype <- rep(1, dataNum)
metricIdx <- which(colnames(all[[1]])==metricName) 

plotWidth <- 5
plotHeight <- 4.7
postscript(outfile, width=plotWidth, height=plotHeight, horizontal=F, paper='special')


maxScore <- 0
minScore <- 0.10 

for (i in 1:dataNum) {
	data <- all[[i]]
	maxScore <- max(data[,metricIdx], maxScore)
	minScore <- min(data[,metricIdx], minScore)
}

yMax <- ceiling(maxScore*100)/100.0 
yMin <- floor(minScore*100)/100.0 
yMin= 0.12
#par(mar=c(4, 4, 0.6, 0.6))
data <-all[[1]] 
par(mar=c(5.1,4.1,2.1,2.1))
plot(data$time, data[, metricIdx], type="n", xlab='time', ylab='MAP', ylim=c(yMin, yMax), main=title)
grid(NULL,NULL, col="gray50", lwd=0.5, lty=4)

for (i in 1:dataNum) {
	data <- all[[i]]
	lines(data$time, data[,metricIdx], col=colors[i], lty=linetype[i],  type='s', lwd=2)
}

#legend(33, yMin + (yMax - yMin) * 0.4, names, col=colors, lty=linetype)
dev.off()
print(outfile)
