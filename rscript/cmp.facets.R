source('functions.R')

args <- commandArgs(T)
feedbackSrc <- args[1]
metricName <- args[2]

#metricName <- "MAP"
#facetSrc <- "annotator"

#feedbackSrc <- "oracle"
feedbackParam <- if (feedbackSrc == "oracle") "sts-0_01" else ""

#feedbackSrc <- "annotator"
#feedbackParam <- ""

expModel <- "ffs" 

outfile <- sprintf("figure/cmp-facet-%s-%s-%s.eps", feedbackSrc, expModel, metricName)
#title <- sprintf("Cmp facets. Feedback=%s, Exp=%s, Metric=%s", feedbackSrc, expModel, metricName)
title <- ""


facetSrc <- "annotator"
facetParam <- getFacetParam(facetSrc)
annotator <- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, expModel)


facetSrc <- "gmi"
facetParam <- getFacetParam(facetSrc)
gmi <- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, expModel)


facetSrc <- "gmj"
facetParam <- getFacetParam(facetSrc)
gmj <- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, expModel)

facetSrc <- "qd"
facetParam <- getFacetParam(facetSrc)
qd <- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, expModel)


facetSrc <- "plsa"
facetParam <- getFacetParam(facetSrc)
plsa <- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, expModel)

facetSrc <- "lda"
facetParam <- getFacetParam(facetSrc)
lda <- loadTcEval(facetSrc, facetParam, feedbackSrc, feedbackParam, expModel)

all <- list(annotator, gmi, gmj, qd, plsa, lda)
names <- c("annotator", "QF-I", "QF-J", "QDM", "pLSA", "LDA")
dataNum <- length(all) 
colors <- rainbow(dataNum)
plotchar <- seq(18,18+dataNum,1)
linetype <- rep(1, dataNum)
metricIdx <- which(colnames(all[[1]])==metricName) 


plotWidth <- 5
plotHeight <- 4.7
postscript(outfile, width=plotWidth, height=plotHeight, horizontal=F, paper='special')


maxScore <- 0
minScore <- 1 

for (i in 1:dataNum) {
	data <- all[[i]]
	maxScore <- max(data[,metricIdx], maxScore)
	minScore <- min(data[,metricIdx], minScore)
}

yMax <- ceiling(maxScore*100)/100.0 
yMin <- floor(minScore*100)/100.0 
#par(mar=c(4, 4, 0.6, 0.6))
data <- all[[1]] 
par(mar=c(5.1,4.1,2.1,2.1))
plot(data$time, data[, metricIdx], type="n", xlab='time', ylab='MAP', ylim=c(yMin, yMax), main=title)
grid(NULL,NULL, col="gray50")

for (i in 1:dataNum) {
	data <- all[[i]]
	lines(data$time, data[,metricIdx], col=colors[i], lty=linetype[i],  type='s', lwd=1.5)
}

legend(28, yMin + (yMax - yMin) * 0.47, names, col=colors, lty=linetype)
#legend(28, 0.22, names, col=colors, lty=linetype, bg="white")
dev.off()
print(outfile)
