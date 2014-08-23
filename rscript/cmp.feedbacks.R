source('functions.R')

args <- commandArgs(T)
facetSrc <- args[1] 
metricName <- args[2]


#metricName <- "MAP"
#feedbacSrc <- ""
#feedbackParam
expModel <- "ffs" 
facetParam <- getFacetParam(facetSrc)

outfile <- sprintf("figure/cmp-feedback-%s-%s-%s.eps", facetSrc, expModel, metricName)
#title <- sprintf("Cmp feedback. Facet=%s, Exp=%s, Metric=%s", facetSrc, expModel, metricName)
title <- ""

oracleSts <- loadTcEval(facetSrc, facetParam, "oracle", "sts-0_01", expModel)
oracleStb <- loadTcEval(facetSrc, facetParam, "oracle", "stb-0_01", expModel)
annotator <- loadTcEval(facetSrc, facetParam, "annotator", "", expModel)

all <- list(oracleSts, oracleStb, annotator)
dataNum <- length(all) 
#names <- c(expression("oracle"["s"]), expression("oracle"["b"]), "annotator")
names <- c("oracle-s", "oracle-b",  "annotator")
colors <- rainbow(dataNum)
plotchar <- seq(18,18+dataNum,1)
linetype <- rep(1, dataNum)
metricIdx <- which(colnames(all[[1]])==metricName) 

plotWidth <- 5
plotHeight <- 4.7
postscript(outfile, width=plotWidth, height=plotHeight, horizontal=F, paper='special')


maxScore <- 0.28
minScore <- 0.15 

for (i in 1:dataNum) {
	data <- all[[i]]
	maxScore <- max(data[,metricIdx], maxScore)
	minScore <- min(data[,metricIdx], minScore)
}

yMax <- ceiling(maxScore*100)/100.0 
yMin <- floor(minScore*100)/100.0 
#par(mar=c(4, 4, 0.6, 0.6))
data <- oracleSts
par(mar=c(5.1,4.1,2.1,2.1))
plot(data$time, data[, metricIdx], type="n", xlab='Time', ylab='MAP', ylim=c(yMin, yMax), main=title)
grid(NULL,NULL, col="gray50")

for (i in 1:dataNum) {
	data <- all[[i]]
	lines(data$time, data[,metricIdx], col=colors[i], lty=linetype[i],  type='s', lwd=1.5)
}

#legend(28, yMin + (yMax - yMin) / 4.0, names, col=colors, lty=linetype, bg="white")
legend(28, 0.195, names, col=colors, lty=linetype, bg="white")
dev.off()
print(outfile)
