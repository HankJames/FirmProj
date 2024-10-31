import numpy as np
import matplotlib.pyplot as plt


def doPlot(dataset1, dataset2):
    combined_file_sizes = np.sort(np.unique(np.concatenate((dataset1, dataset2))))
    cdf1 = compute_cdf(dataset1, combined_file_sizes)
    cdf2 = compute_cdf(dataset2, combined_file_sizes)

    # Step 3: Calculate the absolute differences between the two CDFs at each x-value
    differences = np.abs(cdf1 - cdf2)

    # Step 4: Find the x-value where the difference is maximized
    max_diff_index = np.argmax(differences)
    max_diff_x = combined_file_sizes[max_diff_index]
    max_diff_y1 = cdf1[max_diff_index]
    max_diff_y2 = cdf2[max_diff_index]

    # Step 5: Plot the cumulative distributions for both datasets
    plt.figure(figsize=(10, 6))

    # Plot for dataset1
    plt.plot(combined_file_sizes, cdf1, label='Dataset 1', linestyle='-', linewidth=2, color='blue')

    # Plot for dataset2
    plt.plot(combined_file_sizes, cdf2, label='Dataset 2', linestyle='--', linewidth=2, color='orange')

    # Step 6: Add a vertical auxiliary line at the point of maximum difference
    plt.axvline(x=max_diff_x, color='green', linestyle=':', linewidth=2)
    plt.text(max_diff_x, max_diff_y1, f'({max_diff_x:.1f},{max_diff_y1:.1f}%)', color='blue', ha='right', va='bottom', fontsize=10)
    plt.text(max_diff_x, max_diff_y2, f'({max_diff_x:.1f},{max_diff_y2:.1f}%)', color='orange', ha='left', va='top', fontsize=10)

    # Step 7: Customize the plot
    plt.xlabel('APK File Size (MB)', fontsize=12)
    plt.ylabel('Cumulative Percentage (%)', fontsize=12)
    plt.title('Cumulative Distribution of APK File Sizes', fontsize=16)
    plt.grid(True, which='both', linestyle='--', linewidth=0.5)
    plt.legend()
    plt.tight_layout()

    # Show the plot
    plt.show()


def compute_cdf(data, x_values):
    data_sorted = np.sort(data)
    cdf = np.searchsorted(data_sorted, x_values, side='right') / len(data) * 100
    return cdf


if __name__ == "__main__":
    # Two datasets of APK file sizes (in MB), for demonstration purposes:
    dataset1 = [1.1, 5.2, 3.4, 6.5, 2.2, 8.3, 9.7, 11.3, 15.6, 20.5]  # Replace with actual dataset 1
    dataset2 = [2.5, 7.1, 4.2, 10.0, 14.3, 3.3, 6.8, 25.4, 30.1, 35.9]  # Replace with actual dataset 2
    doPlot(dataset1, dataset2)
