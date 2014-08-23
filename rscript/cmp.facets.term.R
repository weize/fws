source('functions.R')

args <- commandArgs(T)
feedbackSrc <- args[1]
metricName <- "term"

#metricName <- "MAP"
#facetSrc <- "annotator"

#feedbackSrc <- "oracle"
feedbackParam <- if (feedbackSrc == "oracle") "sts-0_01" else ""

#feedbackSrc <- "annotator"
#feedbackParam <- ""

outfile <- sprintf("figure/cmp-facet-%s-%s.eps", feedbackSrc, metricName)
#title <- sprintf("Cmp facets. Feedback=%s, Exp=%s, Metric=%s", feedbackSrc, metricName)
title <- ""


facetSrc <- "annotator"
facetParam <- getFacetParam(facetSrc)
annotator <- loadTermSelectedEval(facetSrc, facetParam, feedbackSrc, feedbackParam)


facetSrc <- "gmi"
facetParam <- getFacetParam(facetSrc)
gmi <- loadTermSelectedEval(facetSrc, facetParam, feedbackSrc, feedbackParam)


facetSrc <- "gmj"
facetParam <- getFacetParam(facetSrc)
gmj <- loadTermSelectedEval(facetSrc, facetParam, feedbackSrc, feedbackParam)

facetSrc <- "qd"
facetParam <- getFacetParam(facetSrc)
qd <- loadTermSelectedEval(facetSrc, facetParam, feedbackSrc, feedbackParam)


facetSrc <- "plsa"
facetParam <- getFacetParam(facetSrc)
plsa <- loadTermSelectedEval(facetSrc, facetParam, feedbackSrc, feedbackParam)

facetSrc <- "lda"
facetParam <- getFacetParam(facetSrc)
lda <- loadTermSelectedEval(facetSrc, facetParam, feedbackSrc, feedbackParam)

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
minScore <- -1.5

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
plot(data$time, data[, metricIdx], type="n", xlab='time', ylab='#terms', ylim=c(yMin, yMax), main=title)
grid(NULL,NULL, col="gray50")

for (i in 1:dataNum) {
	data <- all[[i]]
	lines(data$time, data[,metricIdx], col=colors[i], lty=linetype[i],  type='s', lwd=1.5)
}

legend(28, yMin + (yMax - yMin) * 0.5, names, col=colors, lty=linetype)
#legend(28, 0.22, names, col=colors, lty=linetype, bg="white")
dev.off()
print(outfile)
