/*
 * This file is part of the GeMTC software for MTC model generation and
 * analysis. GeMTC is distributed from http://drugis.org/gemtc.
 * Copyright (C) 2009-2012 Gert van Valkenhoef.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.drugis.mtc.gui.results;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.drugis.common.gui.FileSaveDialog;
import org.drugis.common.gui.LayoutUtil;
import org.drugis.common.gui.ToStringValueModel;
import org.drugis.common.gui.table.EnhancedTable;
import org.drugis.common.gui.table.TableCopyHandler;
import org.drugis.common.threading.status.TaskTerminatedModel;
import org.drugis.common.validation.BooleanAndModel;
import org.drugis.mtc.MCMCModel;
import org.drugis.mtc.MCMCResults;
import org.drugis.mtc.MCMCResultsEvent;
import org.drugis.mtc.MixedTreatmentComparison;
import org.drugis.mtc.gui.FileNames;
import org.drugis.mtc.gui.MainWindow;
import org.drugis.mtc.model.Treatment;
import org.drugis.mtc.parameterization.BasicParameter;
import org.drugis.mtc.presentation.ConsistencyWrapper;
import org.drugis.mtc.presentation.InconsistencyWrapper;
import org.drugis.mtc.presentation.MTCModelWrapper;
import org.drugis.mtc.presentation.NodeSplitWrapper;
import org.drugis.mtc.presentation.SimulationConsistencyWrapper;
import org.drugis.mtc.presentation.SimulationNodeSplitWrapper;
import org.drugis.mtc.presentation.results.NetworkInconsistencyFactorsTableModel;
import org.drugis.mtc.presentation.results.NetworkRelativeEffectTableModel;
import org.drugis.mtc.presentation.results.NetworkVarianceTableModel;
import org.drugis.mtc.presentation.results.RankProbabilityDataset;
import org.drugis.mtc.presentation.results.RankProbabilityTableModel;
import org.drugis.mtc.summary.QuantileSummary;
import org.drugis.mtc.summary.Summary;
import org.drugis.mtc.util.EmpiricalDensityDataset;
import org.drugis.mtc.util.EmpiricalDensityDataset.PlotParameter;
import org.drugis.mtc.util.MCMCResultsAvailableModel;
import org.drugis.mtc.util.MCMCResultsMemoryUsageModel;
import org.drugis.mtc.util.MCMCResultsWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ResultsComponentFactory {

	public static JTable buildRelativeEffectsTable(final List<Treatment> treatments,
			final MTCModelWrapper<?> wrapper, final boolean isDichotomous,
			final boolean showDescription) {
		final JTable reTable = createTableWithoutHeaders(new NetworkRelativeEffectTableModel(treatments, wrapper));
		reTable.setDefaultRenderer(Object.class, new NetworkRelativeEffectTableCellRenderer(isDichotomous, showDescription));

		return reTable;
	}

	private static JTable createTableWithoutHeaders(NetworkRelativeEffectTableModel dm) {
		final JTable table = new JTable(dm);
		table.setTableHeader(null);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		for (final TableColumn c : Collections.list(table.getColumnModel().getColumns())) {
			c.setMinWidth(170);
			c.setPreferredWidth(170);
		}

		TableCopyHandler.registerCopyAction(table);
		return table;
	}

	public static JTable buildVarianceTable(final MTCModelWrapper<?> wrapper) {
		final JTable varTable = new EnhancedTable(new NetworkVarianceTableModel(wrapper), 150);
		varTable.setDefaultRenderer(QuantileSummary.class, new SummaryCellRenderer());
		return varTable;
	}

	public static JTable buildInconsistencyFactors(final InconsistencyWrapper<?> wrapper, final ValueModel modelConstructedModel) {
		final NetworkInconsistencyFactorsTableModel inconsistencyFactorsTableModel = new NetworkInconsistencyFactorsTableModel(wrapper, modelConstructedModel);
		final EnhancedTable table = new EnhancedTable(inconsistencyFactorsTableModel, 300);
		table.setDefaultRenderer(Summary.class, new SummaryCellRenderer(false));
		return table;
	}

	public static ChartPanel buildRankProbabilityChart(final ConsistencyWrapper<?> wrapper) {
		CategoryDataset dataset = new RankProbabilityDataset(wrapper.getRankProbabilities());
		JFreeChart rankChart = ChartFactory.createBarChart("Rank Probability", "Treatment", "Probability",
				dataset, PlotOrientation.VERTICAL, true, true, false);
		final ChartPanel chartPanel = new ChartPanel(rankChart);
		return chartPanel;
	}

	public static EnhancedTable buildRankProbabilityTable(final ConsistencyWrapper<?> wrapper) {
		final EnhancedTable rankTable = EnhancedTable.createBare(new RankProbabilityTableModel(wrapper.getRankProbabilities()));
		rankTable.setDefaultRenderer(Double.class, new SummaryCellRenderer());
		rankTable.autoSizeColumns();
		return rankTable;
	}

	public static JComponent buildNodeSplitDensityChart(final BasicParameter p, NodeSplitWrapper<?> wrapper, ConsistencyWrapper<?> consistency) {
		if (!(wrapper instanceof SimulationNodeSplitWrapper)) {
			return new JLabel("Can not build density plot based on saved results.");
		}
		final SimulationNodeSplitWrapper<?> splitWrapper = (SimulationNodeSplitWrapper<?>) wrapper;
		splitWrapper.getParameters();
		XYDataset dataset;
		final MCMCResults splitResults = splitWrapper.getModel().getResults();
		if(consistency instanceof SimulationConsistencyWrapper) {
			final SimulationConsistencyWrapper<?> consistencyWrapper = (SimulationConsistencyWrapper<?>) consistency;
			dataset = new EmpiricalDensityDataset(50, new PlotParameter(splitResults, splitWrapper.getDirectEffect()),
					new PlotParameter(splitResults, splitWrapper.getIndirectEffect()),
					new PlotParameter(consistencyWrapper.getModel().getResults(), p));
		} else {
			dataset = new EmpiricalDensityDataset(50, new PlotParameter(splitResults, splitWrapper.getDirectEffect()),
					new PlotParameter(splitResults, splitWrapper.getIndirectEffect()));
		}
		final JFreeChart chart = ChartFactory.createXYLineChart(
	            p.getName() + " density plot", "Relative Effect", "Density",
	            dataset, PlotOrientation.VERTICAL,
	            true, true, false
	        );

        return new ChartPanel(chart);
	}

	/**
	 * @param labelText Descriptive label for this memory usage entry
	 * @param parent Parent for "save samples" dialog
	 */
	public static int buildMemoryUsage(
			final MTCModelWrapper<?> model, final String labelText,
			final PanelBuilder builder, final FormLayout layout, final int row,
			final JFrame parent) {
		final CellConstraints cc = new CellConstraints();
		if(model.isSaved()) {
			LayoutUtil.addRow(layout);
			builder.add(new JLabel(labelText), cc.xy(1, row));
			builder.add(new JLabel("N/A"), cc.xyw(3, row, 7));
			return row + 2;
		} else {
			final MixedTreatmentComparison mtc = model.getModel();

			final MCMCResultsMemoryUsageModel memoryModel = new MCMCResultsMemoryUsageModel(mtc.getResults());
			final JLabel memory = BasicComponentFactory.createLabel(new ToStringValueModel(memoryModel));

			final MCMCResultsAvailableModel resultsAvailableModel = new MCMCResultsAvailableModel(mtc.getResults());
			final TaskTerminatedModel modelTerminated = new TaskTerminatedModel(mtc.getActivityTask());

			final JButton clearButton = new JButton(MainWindow.IMAGELOADER.getIcon(FileNames.ICON_DELETE));
			clearButton.setToolTipText("Clear results");
			final BooleanAndModel modelFinishedAndResults = new BooleanAndModel(Arrays.<ValueModel>asList(modelTerminated, resultsAvailableModel));
			Bindings.bind(clearButton, "enabled",  modelFinishedAndResults);


			final JButton saveButton = new JButton(MainWindow.IMAGELOADER.getIcon(FileNames.ICON_SAVEFILE));
			saveButton.setToolTipText("Save to R-file");
			Bindings.bind(saveButton, "enabled", modelFinishedAndResults);
			saveButton.addActionListener(buildRButtonActionListener(mtc, parent));

			clearButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					mtc.getResults().clear();
					// FIXME: change MCMC contract so clear fires a MCMCResultsClearedEvent
					memoryModel.resultsEvent(new MCMCResultsEvent(mtc.getResults()));
					resultsAvailableModel.resultsEvent(new MCMCResultsEvent(mtc.getResults()));
				}
			});

			LayoutUtil.addRow(layout);
			builder.add(new JLabel(labelText), cc.xy(1, row));
			builder.add(memory, cc.xy(3, row));
			builder.add(clearButton, cc.xy(5, row));
			builder.add(saveButton, cc.xy(7, row));
			return row + 2;
		}
	}

	private static ActionListener buildRButtonActionListener(final MCMCModel model, final JFrame mainWindow) {
		return new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final FileSaveDialog dialog = new FileSaveDialog(mainWindow, "R", "R files") {
					@Override
					public void doAction(final String path, final String extension) {
						try {
							final MCMCResultsWriter writer = new MCMCResultsWriter(model.getResults());
							writer.write(new FileOutputStream(path));
						} catch (final FileNotFoundException e) {
							throw new RuntimeException(e);
						} catch (final IOException e) {
							throw new RuntimeException(e);
						}
					}
				};
				dialog.saveActions();
			}
		};
	}
}
