source('functions.R')

infile <- "../exp/data/cmp-feedback/annotator.feedback.imprv"
# id term map
data <- read.csv(infile, header=F, sep="\t")
utfile <- "figure/annotator-feedback-imprv.esp"
title <- ""

plotWidth <- 5
plotHeight <- 5
postscript(outfile, width=plotWidth, height=plotHeight, horizontal=F, paper='special')

maxScore <- 0.28
minScore <- 0.15 

#par(mar=c(4, 4, 0.6, 0.6))
data <- oracleSts
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
