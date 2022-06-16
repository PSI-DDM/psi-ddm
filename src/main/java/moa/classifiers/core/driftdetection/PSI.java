package moa.classifiers.core.driftdetection;

import java.util.ArrayList;
import java.util.List;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class PSI extends AbstractChangeDetector {

	private static final long serialVersionUID = 1L;

	private static int WINDOW_SIZE = 40;
	private static float MIN_VALUE_THRESHOLD = 0.1f;
	private static float SIG_VALUE_THRESHOLD = 0.25f;
	
	public IntOption windowsSize = new IntOption("windowsSize", 'w', "Sliding windows size", WINDOW_SIZE);
	public FloatOption minValueThreshold = new  FloatOption("minValueThreshold", 'm', "Minimal value to detect change", MIN_VALUE_THRESHOLD);
	public FloatOption sigValueThreshold = new FloatOption("sigValueThreshold", 's', "Significant value to detect change", SIG_VALUE_THRESHOLD);

	private List<Double> firstWindow = new ArrayList<Double>();
	private List<Double> secondWindow = new ArrayList<Double>();
	private WindowsStatus windowsStatus = WindowsStatus.FIRST_WINDOW_IN_FILL;
	
	@Override
	public void resetLearning() {
		this.windowsSize.setValue(WINDOW_SIZE);
		this.minValueThreshold.setValue(MIN_VALUE_THRESHOLD);
		this.sigValueThreshold.setValue(SIG_VALUE_THRESHOLD);
		this.firstWindow.clear();
		this.secondWindow.clear();
		this.windowsStatus = WindowsStatus.FIRST_WINDOW_IN_FILL;
	}
	
	@Override
	public void input(double inputValue) {
		addToWindow(inputValue);
		toUpdateWindowsStatus();
		if(this.windowsStatus != WindowsStatus.FILLED_WINDOWS) {
			return;
		}
		double psi = psiValue();
		if(psi > this.minValueThreshold.getValue()) {
			this.isWarningZone = Boolean.TRUE;
		}
		if(psi > this.sigValueThreshold.getValue()) {
			this.isChangeDetected = Boolean.TRUE;
		}	
	}

	private void addToWindow(double inputValue) {
		if(this.windowsStatus == WindowsStatus.FIRST_WINDOW_IN_FILL) {
			this.firstWindow.add(inputValue);
		} else if(this.windowsStatus == WindowsStatus.SECOND_WINDOW_IN_FILL) {
			this.secondWindow.add(inputValue);
		} else {
			toSlideWindows(inputValue);
		}
	}

	private double psiValue() {
		double sumFirstWindow = firstWindow.stream().reduce((base, target) -> base.doubleValue() + target.doubleValue()).get();
		double sumSecondWindow = secondWindow.stream().reduce((base, target) -> base.doubleValue() + target.doubleValue()).get();
		double percentFirtWindow = sumFirstWindow / this.windowsSize.getValue();
		double percentSecondWindow = sumSecondWindow / this.windowsSize.getValue();
		return (percentFirtWindow - percentSecondWindow) * Math.log10(percentFirtWindow / percentSecondWindow);
	}

	private void toSlideWindows(double newInputValue) {
		this.firstWindow.remove(0);
		this.firstWindow.add(this.secondWindow.remove(0));
		this.secondWindow.add(newInputValue);
	}

	private void toUpdateWindowsStatus() {
		if(this.firstWindow.size() < this.windowsSize.getValue()) {
			this.windowsStatus = WindowsStatus.FIRST_WINDOW_IN_FILL;
			return;
		}
		if(this.secondWindow.size() < this.windowsSize.getValue()) {
			this.windowsStatus= WindowsStatus.SECOND_WINDOW_IN_FILL;
			return;
		}
		this.windowsStatus = WindowsStatus.FILLED_WINDOWS;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		// TODO Auto-generated method stub
	}
	
	public enum WindowsStatus {
		FIRST_WINDOW_IN_FILL,
		SECOND_WINDOW_IN_FILL,
		FILLED_WINDOWS;
	}

}
