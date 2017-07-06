package xdomainnlp.api;

import java.math.BigDecimal;

public class NoveltyParameter {
	private boolean noveltyComputed;
	private double weight;
	private double threshold;

	public NoveltyParameter(boolean noveltyComputed, BigDecimal weight, BigDecimal threshold) {
		this.noveltyComputed = noveltyComputed;
		this.weight = weight.doubleValue();
		this.threshold = threshold.doubleValue();
	}

	public boolean isNoveltyComputed() {
		return noveltyComputed;
	}

	public void setNoveltyComputed(boolean noveltyComputed) {
		this.noveltyComputed = noveltyComputed;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getThreshold() {
		return this.threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

}
