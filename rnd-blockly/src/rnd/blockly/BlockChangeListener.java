/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rnd.blockly;

import java.util.List;

/**
 *
 * @author saliya
 */
/**
 * Simple callback interface replacing Consumer<List<Block>> for Java 6/7
 * source-level compatibility.
 */
public interface BlockChangeListener {

    void onBlocksChanged(List<Block> blocks);
}
